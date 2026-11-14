package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.model.TrashItem
import com.anjo.filebrowser.ui.model.dto.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.http.quote
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val UPLOAD_PART = "uploadFile"

class HttpFilebrowserApi(
    private val client: HttpClient,
    baseUrl: String,
) : FilebrowserApi {
    private val api = "$baseUrl/api/v1"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listEntries(
        path: String,
        name: String?,
    ): List<Entry> =
        call {
            client
                .get("$api/entries") {
                    parameter("path", path)
                    parameter("name", name)
                }.decode()
        }

    override suspend fun download(entry: Entry): ByteArray =
        call {
            client
                .get("$api/files/${entry.name.encodeURLPathPart()}") { parameter("path", entry.parent) }
                .checked()
                .body()
        }

    override suspend fun upload(
        directory: String,
        file: PickedFile,
        overwrite: Boolean,
    ) {
        call {
            client
                .post("$api/files") {
                    parameter("path", directory)
                    parameter("overwrite", overwrite)
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    UPLOAD_PART,
                                    file.bytes,
                                    Headers.build { append(HttpHeaders.ContentDisposition, "filename=${file.name.quote()}") },
                                )
                            },
                        ),
                    )
                }.checked()
        }
    }

    override suspend fun createFolder(
        parent: String,
        name: String,
    ) {
        call {
            client
                .post("$api/directories") {
                    parameter("directoryName", name)
                    parameter("path", parent)
                }.checked()
        }
    }

    override suspend fun rename(
        entry: Entry,
        newName: String,
    ) {
        call {
            client
                .patch(if (entry.directory) "$api/directories" else "$api/files") {
                    parameter("sourcePath", entry.path)
                    parameter(if (entry.directory) "newDirName" else "newFileName", newName)
                }.checked()
        }
    }

    override suspend fun delete(entry: Entry) {
        call {
            if (entry.directory) {
                client.delete("$api/directories/recursively") { parameter("path", entry.path) }
            } else {
                client.delete("$api/files/${entry.name.encodeURLPathPart()}") { parameter("path", entry.parent) }
            }.checked()
        }
    }

    override suspend fun trash(): List<TrashItem> = call { client.get("$api/trash").decode() }

    override suspend fun restore(item: TrashItem) {
        call { client.post("$api/trash/${item.id.encodeURLPathPart()}/restore").checked() }
    }

    override suspend fun deleteForever(item: TrashItem) {
        call { client.delete("$api/trash/${item.id.encodeURLPathPart()}").checked() }
    }

    override suspend fun emptyTrash() {
        call { client.delete("$api/trash").checked() }
    }

    override suspend fun quota(): Quota = call { client.get("$api/quota").decode() }

    private suspend fun <T> call(block: suspend () -> T): T =
        try {
            block()
        } catch (e: ApiException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            throw ApiException(ApiStatus.FAILED, cause = e)
        } catch (e: Exception) {
            throw ApiException(ApiStatus.OFFLINE, cause = e)
        }

    private suspend inline fun <reified T> HttpResponse.decode(): T = json.decodeFromString(checked().bodyAsText())

    private suspend fun HttpResponse.checked(): HttpResponse {
        if (status.isSuccess()) return this
        val text = bodyAsText()
        val message = runCatching { json.decodeFromString<ApiError>(text).message }.getOrNull()
        throw ApiException(statusOf(status), message)
    }

    private fun statusOf(status: HttpStatusCode): ApiStatus =
        when (status) {
            HttpStatusCode.BadRequest -> ApiStatus.INVALID
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ApiStatus.UNAUTHORIZED
            HttpStatusCode.NotFound -> ApiStatus.NOT_FOUND
            HttpStatusCode.Conflict -> ApiStatus.CONFLICT
            HttpStatusCode.PayloadTooLarge -> ApiStatus.TOO_LARGE
            HttpStatusCode.InsufficientStorage -> ApiStatus.NO_SPACE
            else -> ApiStatus.FAILED
        }
}
