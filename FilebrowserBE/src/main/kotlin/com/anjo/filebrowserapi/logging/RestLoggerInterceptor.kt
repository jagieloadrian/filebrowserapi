package com.anjo.filebrowserapi.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor

private val SHARE_TOKEN = Regex("/share/[^/]+")

class RestLoggerInterceptor : HandlerInterceptor {
    private val logger = KotlinLogging.logger {}

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        logger.info { "Incoming request: Method = ${request.method}; URL: ${request.requestURL.replace(SHARE_TOKEN, "/share/***")}" }
        request.setAttribute("startTime", System.currentTimeMillis())
        return super.preHandle(request, response, handler)
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val startTime = request.getAttribute("startTime") as Long
        val endTime = System.currentTimeMillis()
        val timeTaken = endTime - startTime
        logger.info { "Outgoing response: Status = ${response.status} execute in: $timeTaken ms" }
        super.afterCompletion(request, response, handler, ex)
    }
}
