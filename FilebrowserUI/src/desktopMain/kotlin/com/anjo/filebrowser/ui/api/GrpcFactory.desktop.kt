package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.TlsChannelCredentials
import java.net.URI

private const val HTTPS = "https"
private const val HTTP_PORT = 80
private const val HTTPS_PORT = 443

actual fun grpcFilebrowserApi(backendUrl: String): FilebrowserApi {
    val uri = URI(backendUrl)
    val tls = uri.scheme == HTTPS
    val host = requireNotNull(uri.host) { "The gRPC backendUrl needs a host, for example http://localhost:9090: '$backendUrl'" }
    val port = uri.port.takeIf { it != -1 } ?: if (tls) HTTPS_PORT else HTTP_PORT
    val credentials = if (tls) TlsChannelCredentials.create() else InsecureChannelCredentials.create()
    return GrpcFilebrowserApi(FilebrowserApiCoroutineStub(Grpc.newChannelBuilderForAddress(host, port, credentials).build()))
}
