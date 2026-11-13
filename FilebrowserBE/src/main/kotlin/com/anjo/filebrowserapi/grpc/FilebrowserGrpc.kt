package com.anjo.filebrowserapi.grpc

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.Filebrowser.FileResponse
import com.anjo.filebrowser.Filebrowser.StringResponse
import com.anjo.filebrowser.Filebrowser.StringsResponse
import com.anjo.filebrowser.FilebrowserApiGrpcKt.FilebrowserApiCoroutineImplBase
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.model.Share
import com.anjo.filebrowserapi.service.DirectoryManager
import com.anjo.filebrowserapi.service.EntryManager
import com.anjo.filebrowserapi.service.FileManager
import com.anjo.filebrowserapi.service.QuotaManager
import com.anjo.filebrowserapi.service.ShareManager
import com.anjo.filebrowserapi.service.TrashManager
import com.anjo.filebrowserapi.service.VersionManager
import com.google.protobuf.Empty
import com.google.protobuf.Timestamp
import com.google.protobuf.kotlin.toByteString
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.time.Instant

internal const val CHUNK_SIZE = 64 * 1024

@Profile("server")
@Service
class FilebrowserGrpc(
    private val directoryManager: DirectoryManager,
    private val fileManager: FileManager,
    private val entryManager: EntryManager,
    private val trashManager: TrashManager,
    private val versionManager: VersionManager,
    private val shareManager: ShareManager,
    private val quotaManager: QuotaManager,
    private val properties: AppConfiguration,
) : FilebrowserApiCoroutineImplBase() {
    override suspend fun showFilesTree(request: Filebrowser.PathRequest): StringsResponse {
        val fileTree = fileManager.showFilesTree(request.path)
        return StringsResponse
            .newBuilder()
            .addAllDirectories(fileTree)
            .build()
    }

    override suspend fun uploadFile(requests: Flow<Filebrowser.UploadFile>): StringResponse =
        withContext(Dispatchers.IO) {
            val spool = Files.createTempFile("upload-", null)
            try {
                var head: Filebrowser.UploadFile? = null
                var size = 0L
                Files.newOutputStream(spool).use { out ->
                    requests.collect { chunk ->
                        size += chunk.data.size()
                        if (size > properties.maxUploadSize.toBytes()) {
                            throw StatusException(Status.RESOURCE_EXHAUSTED.withDescription("Upload exceeds the maximum upload size"))
                        }
                        head = head ?: chunk
                        chunk.data.writeTo(out)
                    }
                }
                val metadata = head ?: throw BadInputException("Upload stream is empty")
                val path =
                    Files.newInputStream(spool).use {
                        fileManager.uploadFile(metadata.name, it, size, metadata.path, metadata.overwrite)
                    }
                StringResponse
                    .newBuilder()
                    .setPath(path)
                    .build()
            } finally {
                Files.deleteIfExists(spool)
            }
        }

    override fun getFile(request: Filebrowser.FileOperation): Flow<FileResponse> =
        flow {
            fileManager.getFile(request.name, request.path).inputStream().use { input ->
                generateSequence { input.readNBytes(CHUNK_SIZE).takeIf { it.isNotEmpty() } }.forEach { chunk ->
                    emit(
                        FileResponse
                            .newBuilder()
                            .setData(chunk.toByteString())
                            .build(),
                    )
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun renameFile(request: Filebrowser.FileOperation): StringResponse {
        val filePath = fileManager.renameFile(request.name, request.path)
        return StringResponse
            .newBuilder()
            .setPath(filePath)
            .build()
    }

    override suspend fun deleteFile(request: Filebrowser.FileOperation): Empty {
        fileManager.deleteFile(request.name, request.path)
        return Empty
            .newBuilder()
            .build()
    }

    override suspend fun showDirectoriesTree(request: Filebrowser.PathRequest): StringsResponse {
        val allDirectories = directoryManager.showDirectoriesTree(request.path)
        return StringsResponse
            .newBuilder()
            .addAllDirectories(allDirectories)
            .build()
    }

    override suspend fun addDirectory(request: Filebrowser.FileOperation): StringResponse {
        val directoryPath = directoryManager.addDirectory(request.name, request.path)
        return StringResponse
            .newBuilder()
            .setPath(directoryPath)
            .build()
    }

    override suspend fun addDirectoriesRecursively(request: Filebrowser.PathRequest): StringResponse {
        val directoryPath = directoryManager.addDirectoriesRecursively(request.path)
        return StringResponse
            .newBuilder()
            .setPath(directoryPath)
            .build()
    }

    override suspend fun renameDirectory(request: Filebrowser.FileOperation): StringResponse {
        val directoryPath = directoryManager.renameDirectory(request.name, request.path)
        return StringResponse
            .newBuilder()
            .setPath(directoryPath)
            .build()
    }

    override suspend fun deleteDirectory(request: Filebrowser.PathRequest): Empty {
        directoryManager.deleteDirectory(request.path)
        return Empty
            .newBuilder()
            .build()
    }

    override suspend fun deleteDirectoryRecursively(request: Filebrowser.PathRequest): Empty {
        directoryManager.deleteDirectoryRecursively(request.path)
        return Empty.getDefaultInstance()
    }

    override suspend fun listEntries(request: Filebrowser.EntriesRequest): Filebrowser.EntriesResponse =
        Filebrowser.EntriesResponse
            .newBuilder()
            .addAllEntries(entryManager.listEntries(request.path, request.name, request.extension, request.tag).map { it.toProto() })
            .build()

    override suspend fun updateEntryMeta(request: Filebrowser.EntryMetaUpdate): Filebrowser.Entry =
        entryManager.updateMeta(request.path, request.description, request.tagsList).toProto()

    override suspend fun findDuplicates(request: Filebrowser.PathRequest): Filebrowser.DuplicatesResponse =
        Filebrowser.DuplicatesResponse
            .newBuilder()
            .addAllGroups(
                entryManager.findDuplicates(request.path).map {
                    Filebrowser.DuplicateGroup
                        .newBuilder()
                        .setSha256(it.sha256)
                        .setSize(it.size)
                        .addAllPaths(it.paths)
                        .build()
                },
            ).build()

    override suspend fun listTrash(request: Empty): Filebrowser.TrashResponse =
        Filebrowser.TrashResponse
            .newBuilder()
            .addAllItems(
                trashManager.list().map {
                    Filebrowser.TrashItem
                        .newBuilder()
                        .setId(it.id.toString())
                        .setPath(it.path)
                        .setDirectory(it.directory)
                        .setSize(it.size)
                        .setDeletedAt(it.deletedAt.toProto())
                        .build()
                },
            ).build()

    override suspend fun restoreTrash(request: Filebrowser.TrashId): StringResponse =
        StringResponse
            .newBuilder()
            .setPath(trashManager.restore(request.id))
            .build()

    override suspend fun purgeTrash(request: Filebrowser.TrashId): Empty {
        trashManager.purge(request.id)
        return Empty.getDefaultInstance()
    }

    override suspend fun emptyTrash(request: Empty): Empty {
        trashManager.empty()
        return Empty.getDefaultInstance()
    }

    override suspend fun listVersions(request: Filebrowser.PathRequest): Filebrowser.VersionsResponse =
        Filebrowser.VersionsResponse
            .newBuilder()
            .addAllVersions(
                versionManager.list(request.path).map {
                    Filebrowser.FileVersion
                        .newBuilder()
                        .setId(it.id.toString())
                        .setSize(it.size)
                        .setCreatedAt(it.createdAt.toProto())
                        .build()
                },
            ).build()

    override suspend fun restoreVersion(request: Filebrowser.VersionId): StringResponse =
        StringResponse
            .newBuilder()
            .setPath(versionManager.restore(request.id))
            .build()

    override suspend fun createShare(request: Filebrowser.ShareRequest): Filebrowser.CreatedShare {
        val created = shareManager.create(request.path, request.takeIf { it.hasDays() }?.days)
        return Filebrowser.CreatedShare
            .newBuilder()
            .setShare(created.share.toProto())
            .setToken(created.token)
            .build()
    }

    override suspend fun listShares(request: Empty): Filebrowser.SharesResponse =
        Filebrowser.SharesResponse
            .newBuilder()
            .addAllShares(shareManager.list().map { it.toProto() })
            .build()

    override suspend fun revokeShare(request: Filebrowser.ShareId): Empty {
        shareManager.revoke(request.id)
        return Empty.getDefaultInstance()
    }

    override suspend fun getQuota(request: Empty): Filebrowser.Quota {
        val quota = quotaManager.quota()
        return Filebrowser.Quota
            .newBuilder()
            .setUsed(quota.used)
            .setLimit(quota.limit)
            .build()
    }

    override suspend fun copyEntry(request: Filebrowser.EntryTransfer): StringResponse =
        StringResponse
            .newBuilder()
            .setPath(entryManager.copyEntry(request.source, request.target))
            .build()

    override suspend fun moveEntry(request: Filebrowser.EntryTransfer): StringResponse =
        StringResponse
            .newBuilder()
            .setPath(entryManager.moveEntry(request.source, request.target))
            .build()

    private fun Entry.toProto(): Filebrowser.Entry =
        Filebrowser.Entry
            .newBuilder()
            .setPath(path)
            .setDirectory(directory)
            .setSize(size)
            .setModified(modified.toProto())
            .setContentType(contentType.orEmpty())
            .setDescription(description.orEmpty())
            .addAllTags(tags)
            .build()

    private fun Share.toProto(): Filebrowser.Share =
        Filebrowser.Share
            .newBuilder()
            .setId(id.toString())
            .setPath(path)
            .setCreatedAt(createdAt.toProto())
            .setExpiresAt(expiresAt.toProto())
            .build()

    private fun Instant.toProto(): Timestamp =
        Timestamp
            .newBuilder()
            .setSeconds(epochSecond)
            .setNanos(nano)
            .build()
}
