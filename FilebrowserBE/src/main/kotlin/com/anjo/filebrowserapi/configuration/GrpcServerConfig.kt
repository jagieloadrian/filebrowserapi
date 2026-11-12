package com.anjo.filebrowserapi.configuration

import com.anjo.filebrowserapi.exception.GrpcGlobalControllerAdvice
import com.anjo.filebrowserapi.grpc.FilebrowserGrpc
import com.anjo.filebrowserapi.logging.GrpcLoggingInterceptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import jakarta.annotation.PreDestroy
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.concurrent.TimeUnit

private const val SHUTDOWN_TIMEOUT_SECONDS = 15L

@Configuration
@Profile("server")
class GrpcServerConfig(
    private val properties: GrpcServerProperties,
    private val filebrowserGrpc: FilebrowserGrpc,
    private val loggingInterceptor: GrpcLoggingInterceptor,
    private val globalControllerAdvice: GrpcGlobalControllerAdvice,
) : ApplicationRunner {
    private val logger = KotlinLogging.logger {}
    private lateinit var server: Server

    override fun run(args: ApplicationArguments) {
        server =
            ServerBuilder
                .forPort(properties.port)
                .addService(filebrowserGrpc)
                .intercept(loggingInterceptor)
                .intercept(globalControllerAdvice)
                .build()
                .start()
        logger.info { "gRPC server started on port: ${properties.port}" }
    }

    @PreDestroy
    fun shutdown() {
        if (!::server.isInitialized) return
        logger.info { "Shutting down gRPC server..." }
        if (!server.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            server.shutdownNow()
        }
        logger.info { "gRPC server shut down" }
    }
}
