package com.anjo.filebrowserapi.configuration

import com.anjo.filebrowserapi.exception.GrpcGlobalControllerAdvice
import com.anjo.filebrowserapi.grpc.FilebrowserGrpc
import com.anjo.filebrowserapi.logging.GrpcLoggingInterceptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import kotlin.concurrent.thread

@Configuration
@Profile("server")
class GrpcServerConfig(
    private val filebrowserGrpc: FilebrowserGrpc,
    private val loggingInterceptor: GrpcLoggingInterceptor,
    private val globalControllerAdvice: GrpcGlobalControllerAdvice,
) : ApplicationRunner {
    private val logger = KotlinLogging.logger {}

    @Value("\${grpc.server.port:9090}")
    private val port: Int = 9090
    private lateinit var server: Server

    fun runGrpcServer(): Server {
        server =
            ServerBuilder
                .forPort(port)
                .addService(filebrowserGrpc)
                .intercept(loggingInterceptor)
                .intercept(globalControllerAdvice)
                .build()
        server.start()
        return server
    }

    @PreDestroy
    fun shutdown() {
        if (::server.isInitialized) {
            logger.info { "Shutting down gRPC server..." }
            server.shutdown()
            logger.info { "gRPC server shut down" }
        }
    }

    override fun run(args: ApplicationArguments?) {
        runGrpcServer()
        logger.info { "gRPC server start on port: $port" }
        thread(name = "grpc-await-termination") { server.awaitTermination() }
    }
}
