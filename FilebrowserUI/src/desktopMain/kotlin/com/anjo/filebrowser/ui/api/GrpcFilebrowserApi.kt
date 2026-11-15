package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub
import com.anjo.filebrowser.entriesRequest
import com.anjo.filebrowser.fileOperation
import com.anjo.filebrowser.pathRequest
import com.anjo.filebrowser.trashId
import com.anjo.filebrowser.ui.auth.AccessTokens
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.model.TrashItem
import com.anjo.filebrowser.uploadFile
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.google.protobuf.Timestamp
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

private const val CHUNK_SIZE = 64 * 1024
private val AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

class GrpcFilebrowserApi(
    private val stub: FilebrowserApiCoroutineStub,
    private val tokens: AccessTokens,
) : FilebrowserApi {
    override suspend fun listEntries(
        path: String,
        name: String?,
    ): List<Entry> =
        call {
            it
                .listEntries(
                    entriesRequest {
                        this.path = path
                        name?.let { this.name = it }
                    },
                ).entriesList
                .map { Entry(it.path, it.directory, it.size, it.modified.toInstant(), it.contentType.ifEmpty { null }) }
        }

    override suspend fun download(
        entry: Entry,
        maxBytes: Long?,
    ): ByteArray =
        call {
            val content = ByteArrayOutputStream()
            it
                .getFile(operation(entry.name, entry.parent))
                .takeWhile { content.size() < (maxBytes ?: Long.MAX_VALUE) }
                .collect { it.data.writeTo(content) }
            content.toByteArray()
        }

    override suspend fun upload(
        directory: String,
        file: PickedFile,
        overwrite: Boolean,
    ) {
        call {
            val chunks =
                (0 until maxOf(file.bytes.size, 1) step CHUNK_SIZE).asFlow().map { offset ->
                    uploadFile {
                        data = ByteString.copyFrom(file.bytes, offset, minOf(CHUNK_SIZE, file.bytes.size - offset))
                        name = file.name
                        path = directory
                        this.overwrite = overwrite
                    }
                }
            it.uploadFile(chunks)
        }
    }

    override suspend fun createFolder(
        parent: String,
        name: String,
    ) {
        call { it.addDirectory(operation(name, parent)) }
    }

    override suspend fun rename(
        entry: Entry,
        newName: String,
    ) {
        call {
            val request = operation(newName, entry.path)
            if (entry.directory) it.renameDirectory(request) else it.renameFile(request)
        }
    }

    override suspend fun delete(entry: Entry) {
        call {
            if (entry.directory) {
                it.deleteDirectoryRecursively(pathRequest { path = entry.path })
            } else {
                it.deleteFile(operation(entry.name, entry.parent))
            }
        }
    }

    override suspend fun trash(): List<TrashItem> =
        call {
            it.listTrash(Empty.getDefaultInstance()).itemsList.map {
                TrashItem(it.id, it.path, it.directory, it.size, it.deletedAt.toInstant())
            }
        }

    override suspend fun restore(item: TrashItem) {
        call { it.restoreTrash(trashId { id = item.id }) }
    }

    override suspend fun deleteForever(item: TrashItem) {
        call { it.purgeTrash(trashId { id = item.id }) }
    }

    override suspend fun emptyTrash() {
        call { it.emptyTrash(Empty.getDefaultInstance()) }
    }

    override suspend fun quota(): Quota = call { it.getQuota(Empty.getDefaultInstance()).let { Quota(it.used, it.limit) } }

    private suspend fun <T> call(block: suspend (FilebrowserApiCoroutineStub) -> T): T =
        try {
            val token = tokens.current()
            try {
                block(stub.bearer(token))
            } catch (e: StatusException) {
                val renewed = token?.takeIf { e.status.code == Status.Code.UNAUTHENTICATED }?.let { tokens.rejected(it) } ?: throw e
                block(stub.bearer(renewed))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val status = Status.fromThrowable(e)
            throw ApiException(status.code.apiStatus(), status.description, e)
        }

    private fun operation(
        name: String,
        path: String,
    ): Filebrowser.FileOperation =
        fileOperation {
            this.name = name
            this.path = path
        }

    private fun FilebrowserApiCoroutineStub.bearer(token: String?): FilebrowserApiCoroutineStub =
        token?.let { withInterceptors(MetadataUtils.newAttachHeadersInterceptor(Metadata().apply { put(AUTHORIZATION, "Bearer $it") })) }
            ?: this

    private fun Timestamp.toInstant() = Instant.fromEpochSeconds(seconds, nanos)

    private fun Status.Code.apiStatus(): ApiStatus =
        when (this) {
            Status.Code.INVALID_ARGUMENT -> ApiStatus.INVALID
            Status.Code.UNAUTHENTICATED -> ApiStatus.SIGNED_OUT
            Status.Code.PERMISSION_DENIED -> ApiStatus.FORBIDDEN
            Status.Code.NOT_FOUND -> ApiStatus.NOT_FOUND
            Status.Code.ALREADY_EXISTS, Status.Code.FAILED_PRECONDITION -> ApiStatus.CONFLICT
            Status.Code.RESOURCE_EXHAUSTED -> ApiStatus.NO_SPACE
            Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED -> ApiStatus.OFFLINE
            else -> ApiStatus.FAILED
        }
}
