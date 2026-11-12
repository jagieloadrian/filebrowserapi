package com.anjo.filebrowserapi.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component

@Component
@GlobalServerInterceptor
@Order(20)
class GrpcLoggingInterceptor : ServerInterceptor {
    private val logger = KotlinLogging.logger {}

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>?,
    ): ServerCall.Listener<ReqT> {
        val methodName = call?.methodDescriptor?.fullMethodName
        logger.info { "Received gRPC call: $methodName" }

        val startTime = System.currentTimeMillis()
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
            next?.startCall(call, headers),
        ) {
            override fun onComplete() {
                val duration = System.currentTimeMillis() - startTime
                logger.info { "Completed gRPC call: $methodName in ${duration}ms" }
                super.onComplete()
            }

            override fun onCancel() {
                logger.warn { "gRPC call cancelled: $methodName" }
                super.onCancel()
            }
        }
    }
}
