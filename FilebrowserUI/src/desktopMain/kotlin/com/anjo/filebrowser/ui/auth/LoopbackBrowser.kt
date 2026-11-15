package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.AuthCallback
import com.anjo.filebrowser.ui.model.SignInAttempt
import com.sun.net.httpserver.HttpServer
import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI

private const val LOOPBACK = "127.0.0.1"
private const val CALLBACK_PATH = "/callback"
private const val HTTP_OK = 200
private const val DONE_PAGE =
    "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Filebrowser</title></head>" +
        "<body><p>You can close this tab and return to Filebrowser.</p></body></html>"

class LoopbackBrowser(
    private val browse: (URI) -> Unit = { Desktop.getDesktop().browse(it) },
) : AuthBrowser {
    override val returnUri: String? = null

    override suspend fun authorize(
        attempt: SignInAttempt,
        url: (redirectUri: String) -> String,
    ): AuthCallback {
        val query = CompletableDeferred<Parameters>()
        val server = HttpServer.create(InetSocketAddress(InetAddress.getByName(LOOPBACK), 0), 0)
        server.createContext(CALLBACK_PATH) { exchange ->
            query.complete(parseQueryString(exchange.requestURI.rawQuery.orEmpty()))
            val page = DONE_PAGE.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(HTTP_OK, page.size.toLong())
            exchange.responseBody.use { it.write(page) }
        }
        server.start()
        try {
            val redirectUri = "http://$LOOPBACK:${server.address.port}$CALLBACK_PATH"
            open(url(redirectUri))
            return AuthCallback(attempt, redirectUri, query.await())
        } finally {
            server.stop(0)
        }
    }

    override fun callback(): AuthCallback? = null

    override suspend fun open(url: String) {
        withContext(Dispatchers.IO) {
            try {
                browse(URI(url))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw SignInException(BROWSER_UNAVAILABLE, e)
            }
        }
    }
}

actual fun authBrowser(): AuthBrowser = LoopbackBrowser()
