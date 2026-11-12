package com.anjo.filebrowserapi.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GrpcLoggingInterceptorTest {
    private val appender = ListAppender<ILoggingEvent>().apply { start() }
    private val logger = LoggerFactory.getLogger(GrpcLoggingInterceptor::class.java) as Logger

    @BeforeTest
    fun attachAppender() {
        logger.addAppender(appender)
    }

    @AfterTest
    fun detachAppender() {
        logger.detachAppender(appender)
    }

    @Test
    fun `should log only the method name when call is received`() {
        // given
        val headers =
            Metadata().apply {
                put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer $TOKEN")
            }
        val call = mockk<ServerCall<String, String>>(relaxed = true)
        every { call.methodDescriptor.fullMethodName } returns METHOD
        val next = mockk<ServerCallHandler<String, String>>(relaxed = true)

        // when
        GrpcLoggingInterceptor().interceptCall(call, headers, next)

        // then
        appender.list shouldHaveSize 1
        appender.list.first().formattedMessage shouldBe "Received gRPC call: $METHOD"
    }

    private companion object {
        const val TOKEN = "secret-token"
        const val METHOD = "com.anjo.filebrowser.FilebrowserApi/getFile"
    }
}
