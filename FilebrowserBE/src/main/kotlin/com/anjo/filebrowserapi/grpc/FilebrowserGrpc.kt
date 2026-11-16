package com.anjo.filebrowserapi.grpc

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.Filebrowser.FileResponse
import com.anjo.filebrowser.Filebrowser.StringResponse
import com.anjo.filebrowser.Filebrowser.StringsResponse
import com.anjo.filebrowser.FilebrowserApiGrpcKt.FilebrowserApiCoroutineImplBase
import com.anjo.filebrowser.createdShare
import com.anjo.filebrowser.duplicateGroup
import com.anjo.filebrowser.duplicatesResponse
import com.anjo.filebrowser.entriesResponse
import com.anjo.filebrowser.entry
import com.anjo.filebrowser.fileResponse
import com.anjo.filebrowser.fileVersion
import com.anjo.filebrowser.quota
import com.anjo.filebrowser.share
import com.anjo.filebrowser.sharesResponse
import com.anjo.filebrowser.stringResponse
import com.anjo.filebrowser.stringsResponse
import com.anjo.filebrowser.trashItem
import com.anjo.filebrowser.trashResponse
import com.anjo.filebrowser.versionsResponse
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
import com.google.protobuf.timestamp
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
        return stringsResponse { directories += fileTree }
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
                stringResponse { this.path = path }
            } finally {
                Files.deleteIfExists(spool)
            }
        }

    override fun getFile(request: Filebrowser.FileOperation): Flow<FileResponse> =
        flow {
            fileManager.getFile(request.name, request.path).inputStream().use { input ->
                generateSequence { input.readNBytes(CHUNK_SIZE).takeIf { it.isNotEmpty() } }.forEach { chunk ->
                    emit(fileResponse { data = chunk.toByteString() })
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun renameFile(request: Filebrowser.FileOperation): StringResponse {
        val filePath = fileManager.renameFile(request.name, request.path)
        return stringResponse { path = filePath }
    }

    override suspend fun deleteFile(request: Filebrowser.FileOperation): Empty {
        fileManager.deleteFile(request.name, request.path)
        return Empty.getDefaultInstance()
    }

    override suspend fun showDirectoriesTree(request: Filebrowser.PathRequest): StringsResponse {
        val allDirectories = directoryManager.showDirectoriesTree(request.path)
        return stringsResponse { directories += allDirectories }
    }

    override suspend fun addDirectory(request: Filebrowser.FileOperation): StringResponse {
        val directoryPath = directoryManager.addDirectory(request.name, request.path)
        return stringResponse { path = directoryPath }
    }

    override suspend fun addDirectoriesRecursively(request: Filebrowser.PathRequest): StringResponse {
        val directoryPath = directoryManager.addDirectoriesRecursively(request.path)
        return stringResponse { path = directoryPath }
    }

    override suspend fun renameDirectory(request: Filebrowser.FileOperation): StringResponse {
        val directoryPath = directoryManager.renameDirectory(request.name, request.path)
        return stringResponse { path = directoryPath }
    }

    override suspend fun deleteDirectory(request: Filebrowser.PathRequest): Empty {
        directoryManager.deleteDirectory(request.path)
        return Empty.getDefaultInstance()
    }

    override suspend fun deleteDirectoryRecursively(request: Filebrowser.PathRequest): Empty {
        directoryManager.deleteDirectoryRecursively(request.path)
        return Empty.getDefaultInstance()
    }

    override suspend fun listEntries(request: Filebrowser.EntriesRequest): Filebrowser.EntriesResponse =
        entriesResponse {
            entries += entryManager.listEntries(request.path, request.name, request.extension, request.tag).map { it.toProto() }
        }

    override suspend fun updateEntryMeta(request: Filebrowser.EntryMetaUpdate): Filebrowser.Entry =
        entryManager.updateMeta(request.path, request.description, request.tagsList).toProto()

    override suspend fun findDuplicates(request: Filebrowser.PathRequest): Filebrowser.DuplicatesResponse =
        duplicatesResponse {
            groups +=
                entryManager.findDuplicates(request.path).map {
                    duplicateGroup {
                        sha256 = it.sha256
                        size = it.size
                        paths += it.paths
                    }
                }
        }

    override suspend fun listTrash(request: Empty): Filebrowser.TrashResponse =
        trashResponse {
            items +=
                trashManager.list().map {
                    trashItem {
                        id = it.id.toString()
                        path = it.path
                        directory = it.directory
                        size = it.size
                        deletedAt = it.deletedAt.toProto()
                    }
                }
        }

    override suspend fun restoreTrash(request: Filebrowser.TrashId): StringResponse =
        stringResponse { path = trashManager.restore(request.id) }

    override suspend fun purgeTrash(request: Filebrowser.TrashId): Empty {
        trashManager.purge(request.id)
        return Empty.getDefaultInstance()
    }

    override suspend fun emptyTrash(request: Empty): Empty {
        trashManager.empty()
        return Empty.getDefaultInstance()
    }

    override suspend fun listVersions(request: Filebrowser.PathRequest): Filebrowser.VersionsResponse =
        versionsResponse {
            versions +=
                versionManager.list(request.path).map {
                    fileVersion {
                        id = it.id.toString()
                        size = it.size
                        createdAt = it.createdAt.toProto()
                    }
                }
        }

    override suspend fun restoreVersion(request: Filebrowser.VersionId): StringResponse =
        stringResponse { path = versionManager.restore(request.id) }

    override suspend fun createShare(request: Filebrowser.ShareRequest): Filebrowser.CreatedShare {
        val created = shareManager.create(request.path, request.takeIf { it.hasDays() }?.days)
        return createdShare {
            share = created.share.toProto()
            token = created.token
        }
    }

    override suspend fun listShares(request: Empty): Filebrowser.SharesResponse =
        sharesResponse { shares += shareManager.list().map { it.toProto() } }

    override suspend fun revokeShare(request: Filebrowser.ShareId): Empty {
        shareManager.revoke(request.id)
        return Empty.getDefaultInstance()
    }

    override suspend fun getQuota(request: Empty): Filebrowser.Quota {
        val current = quotaManager.quota()
        return quota {
            used = current.used
            limit = current.limit
        }
    }

    override suspend fun copyEntry(request: Filebrowser.EntryTransfer): StringResponse =
        stringResponse { path = entryManager.copyEntry(request.source, request.target) }

    override suspend fun moveEntry(request: Filebrowser.EntryTransfer): StringResponse =
        stringResponse { path = entryManager.moveEntry(request.source, request.target) }

    private fun Entry.toProto(): Filebrowser.Entry =
        entry {
            path = this@toProto.path
            directory = this@toProto.directory
            size = this@toProto.size
            modified = this@toProto.modified.toProto()
            contentType = this@toProto.contentType.orEmpty()
            description = this@toProto.description.orEmpty()
            tags += this@toProto.tags
        }

    private fun Share.toProto(): Filebrowser.Share =
        share {
            id = this@toProto.id.toString()
            path = this@toProto.path
            createdAt = this@toProto.createdAt.toProto()
            expiresAt = this@toProto.expiresAt.toProto()
        }

    private fun Instant.toProto(): Timestamp =
        timestamp {
            seconds = epochSecond
            nanos = nano
        }
}
