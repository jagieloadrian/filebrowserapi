package com.anjo.filebrowserapi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("grpc.server")
data class GrpcServerProperties(
    val port: Int = 9090,
)
