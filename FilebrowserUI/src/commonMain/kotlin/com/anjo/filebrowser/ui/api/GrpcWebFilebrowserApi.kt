package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.EntriesRequest
import com.anjo.filebrowser.EntriesResponse
import com.anjo.filebrowser.FileOperation
import com.anjo.filebrowser.FileResponse
import com.anjo.filebrowser.PathRequest
import com.anjo.filebrowser.TrashId
import com.anjo.filebrowser.TrashResponse
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.model.TrashItem
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import kotlinx.coroutines.CancellationException
import okio.Buffer
import okio.IOException
import kotlin.time.Instant
import com.anjo.filebrowser.Quota as QuotaReply
import com.squareup.wire.Instant as Timestamp

private const val SERVICE = "com.anjo.filebrowser.FilebrowserApi"
private const val X_GRPC_WEB = "x-grpc-web"
private val GRPC_WEB_PROTO = ContentType("application", "grpc-web+proto")

class GrpcWebFilebrowserApi(
    private val client: HttpClient,
    baseUrl: String,
) : FilebrowserApi {
    private val service = "$baseUrl/$SERVICE"
    private val restForClientStreaming = HttpFilebrowserApi(client, baseUrl)

    override suspend fun listEntries(
        path: String,
        name: String?,
    ): List<Entry> =
        unary("listEntries", EntriesRequest(path = path, name = name.orEmpty()), EntriesResponse.ADAPTER)
            .entries
            .map { Entry(it.path, it.directory, it.size, it.modified.toInstant()) }

    override suspend fun download(entry: Entry): ByteArray =
        Buffer()
            .apply { send("getFile", operation(entry.name, entry.parent)).decoded(FileResponse.ADAPTER).forEach { write(it.data_) } }
            .readByteArray()

    override suspend fun upload(
        directory: String,
        file: PickedFile,
        overwrite: Boolean,
    ) = restForClientStreaming.upload(directory, file, overwrite)

    override suspend fun createFolder(
        parent: String,
        name: String,
    ) {
        send("addDirectory", operation(name, parent))
    }

    override suspend fun rename(
        entry: Entry,
        newName: String,
    ) {
        send(if (entry.directory) "renameDirectory" else "renameFile", operation(newName, entry.path))
    }

    override suspend fun delete(entry: Entry) {
        if (entry.directory) {
            send("deleteDirectoryRecursively", PathRequest(path = entry.path))
        } else {
            send("deleteFile", operation(entry.name, entry.parent))
        }
    }

    override suspend fun trash(): List<TrashItem> =
        unary("listTrash", null, TrashResponse.ADAPTER).items.map {
            TrashItem(it.id, it.path, it.directory, it.size, it.deleted_at.toInstant())
        }

    override suspend fun restore(item: TrashItem) {
        send("restoreTrash", TrashId(id = item.id))
    }

    override suspend fun deleteForever(item: TrashItem) {
        send("purgeTrash", TrashId(id = item.id))
    }

    override suspend fun emptyTrash() {
        send("emptyTrash", null)
    }

    override suspend fun quota(): Quota = unary("getQuota", null, QuotaReply.ADAPTER).let { Quota(it.used, it.limit) }

    private suspend fun send(
        method: String,
        request: Message<*, *>?,
    ): List<ByteArray> {
        val response: HttpResponse =
            try {
                client.post("$service/$method") {
                    header(X_GRPC_WEB, "1")
                    setBody(ByteArrayContent(grpcWebFrame(request?.encode() ?: byteArrayOf()), GRPC_WEB_PROTO))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw ApiException(ApiStatus.OFFLINE, cause = e)
            }
        return grpcWebMessages(response.status, response.headers, response.bodyAsBytes())
    }

    private suspend fun <T : Any> unary(
        method: String,
        request: Message<*, *>?,
        reply: ProtoAdapter<T>,
    ): T = send(method, request).decoded(reply).singleOrNull() ?: throw ApiException(ApiStatus.FAILED)

    private fun <T : Any> List<ByteArray>.decoded(adapter: ProtoAdapter<T>): List<T> =
        try {
            map { adapter.decode(it) }
        } catch (e: IOException) {
            throw ApiException(ApiStatus.FAILED, cause = e)
        }

    private fun operation(
        name: String,
        path: String,
    ) = FileOperation(name = name, path = path)

    private fun Timestamp?.toInstant(): Instant =
        this?.let { Instant.fromEpochSeconds(it.getEpochSecond(), it.getNano()) } ?: Instant.fromEpochSeconds(0)
}
