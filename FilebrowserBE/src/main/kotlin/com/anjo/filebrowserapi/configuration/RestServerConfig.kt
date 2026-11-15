package com.anjo.filebrowserapi.configuration

import com.anjo.filebrowserapi.logging.RestLoggerInterceptor
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.util.DigestUtils
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.ConcurrentHashMap

@Configuration
@Profile("web")
@Order(Ordered.HIGHEST_PRECEDENCE)
class RestServerConfig(
    private val webProperties: WebProperties,
) : WebMvcConfigurer {
    private val etags = ConcurrentHashMap<Resource, String>()

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(RestLoggerInterceptor())
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/**")
            .addResourceLocations(*webProperties.resources.staticLocations)
            .setCacheControl(CacheControl.noCache())
            .setUseLastModified(false)
            .setEtagGenerator { resource -> etags.computeIfAbsent(resource, ::contentHash) }
    }

    private fun contentHash(resource: Resource): String = resource.inputStream.use(DigestUtils::md5DigestAsHex)
}
