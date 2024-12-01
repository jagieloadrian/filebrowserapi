package com.anjo.filebrowserapi.configuration

import com.anjo.filebrowserapi.grpc.FilebrowserGrpc
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.annotation.PreDestroy

@Configuration
@Profile("server")
class GrpcServerConfig(private val filebrowserGrpc: FilebrowserGrpc) : ApplicationRunner {
    private val logger = KotlinLogging.logger {}

    @Value("\${grpc.server.port:9090}")
    private val port: Int = 9090
    private lateinit var server: Server


    fun runGrpcServer(): Server {
        server = ServerBuilder
                .forPort(port)
                .addService(filebrowserGrpc)
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
        server.awaitTermination()
    }
}