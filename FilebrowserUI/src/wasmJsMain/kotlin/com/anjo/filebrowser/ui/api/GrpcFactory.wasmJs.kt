package com.anjo.filebrowser.ui.api

private const val GRPC_ON_WEB =
    "The gRPC transport works only on desktop: browsers cannot speak native gRPC without a gRPC-Web proxy. " +
        "Build the web app with -Ptransport=http."

actual fun grpcFilebrowserApi(backendUrl: String): FilebrowserApi = throw UnsupportedOperationException(GRPC_ON_WEB)
