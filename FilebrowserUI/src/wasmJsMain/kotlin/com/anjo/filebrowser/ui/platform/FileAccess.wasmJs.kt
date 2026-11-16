@file:OptIn(ExperimentalWasmJsInterop::class)

package com.anjo.filebrowser.ui.platform

import com.anjo.filebrowser.ui.model.PickedFile
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val REVOKE_DELAY_MS = 1000

actual suspend fun pickFile(): PickedFile? {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    val file =
        suspendCancellableCoroutine<File?> { continuation ->
            fun finish(picked: File?) {
                if (continuation.isActive) continuation.resume(picked)
            }
            input.onchange = { finish(input.files?.item(0)) }
            input.addEventListener("cancel", { finish(null) })
            input.click()
        } ?: return null
    val bytes =
        suspendCancellableCoroutine { continuation ->
            val reader = FileReader()
            reader.onload = {
                val buffer = checkNotNull(reader.result) { "Could not read ${file.name}" }
                continuation.resume(Int8Array(buffer.unsafeCast<ArrayBuffer>()).toByteArray())
            }
            reader.onerror = { continuation.resumeWithException(IllegalStateException("Could not read ${file.name}")) }
            reader.readAsArrayBuffer(file)
        }
    return PickedFile(file.name, bytes)
}

actual suspend fun saveFile(
    name: String,
    content: suspend () -> ByteArray,
) {
    val blob = Blob(listOf<JsAny?>(content().toInt8Array()).toJsArray(), BlobPropertyBag(type = "application/octet-stream"))
    val url = URL.createObjectURL(blob)
    val link = document.createElement("a") as HTMLAnchorElement
    link.href = url
    link.download = name
    link.click()
    window.setTimeout({
        URL.revokeObjectURL(url)
        null
    }, REVOKE_DELAY_MS)
}
