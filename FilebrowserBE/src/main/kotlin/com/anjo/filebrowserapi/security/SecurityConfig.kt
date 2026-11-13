package com.anjo.filebrowserapi.security

import com.anjo.filebrowser.FilebrowserApiGrpc
import com.anjo.filebrowserapi.model.ApiError
import io.grpc.health.v1.HealthGrpc
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor
import org.springframework.grpc.server.security.GrpcSecurity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler
import org.springframework.security.web.SecurityFilterChain
import tools.jackson.databind.json.JsonMapper

const val READER = "filebrowser-reader"
const val WRITER = "filebrowser-writer"
private const val ROLE_PREFIX = "ROLE_"
private const val LOCAL_USER = "local"

private val GRPC_READS =
    listOf(
        FilebrowserApiGrpc.getShowFilesTreeMethod(),
        FilebrowserApiGrpc.getGetFileMethod(),
        FilebrowserApiGrpc.getShowDirectoriesTreeMethod(),
        FilebrowserApiGrpc.getListEntriesMethod(),
        FilebrowserApiGrpc.getFindDuplicatesMethod(),
        FilebrowserApiGrpc.getListTrashMethod(),
    ).map { it.fullMethodName }

private val GRPC_WRITES = FilebrowserApiGrpc.getServiceDescriptor().methods.map { it.fullMethodName } - GRPC_READS.toSet()

@Configuration
@OpenAPIDefinition(security = [SecurityRequirement(name = "bearer")])
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class SecurityConfig {
    @Bean
    @Profile("!local")
    fun jwtAuthenticationConverter(
        @Value("\${spring.security.oauth2.resourceserver.jwt.audiences:}") audiences: List<String>,
    ): JwtAuthenticationConverter {
        require(audiences.isNotEmpty()) { "spring.security.oauth2.resourceserver.jwt.audiences must name the app's client ID" }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(
                JwtGrantedAuthoritiesConverter().apply {
                    setAuthoritiesClaimName("groups")
                    setAuthorityPrefix(ROLE_PREFIX)
                },
            )
        }
    }

    @Bean
    @Profile("web & !local")
    fun webSecurity(
        http: HttpSecurity,
        converter: JwtAuthenticationConverter,
        mapper: JsonMapper,
    ): SecurityFilterChain {
        val entryPoint = BearerTokenAuthenticationEntryPoint()
        val deniedHandler = BearerTokenAccessDeniedHandler()
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/health/**", permitAll)
                authorize("/metrics", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/error", permitAll)
                authorize(HttpMethod.GET, "/api/v1/**", hasAnyRole(READER, WRITER))
                authorize("/api/v1/**", hasRole(WRITER))
                authorize(anyRequest, denyAll)
            }
            oauth2ResourceServer {
                jwt { jwtAuthenticationConverter = converter }
                authenticationEntryPoint = { request, response, exception ->
                    entryPoint.commence(request, response, exception)
                    mapper.writeError(response, exception)
                }
                accessDeniedHandler = { request, response, exception ->
                    deniedHandler.handle(request, response, exception)
                    mapper.writeError(response, exception)
                }
            }
        }
        return http.build()
    }

    @Bean
    @Profile("server & !local")
    @GlobalServerInterceptor
    fun grpcAuthorization(
        grpc: GrpcSecurity,
        converter: JwtAuthenticationConverter,
    ): AuthenticationProcessInterceptor =
        grpc
            .authorizeRequests {
                it
                    .methods("${HealthGrpc.SERVICE_NAME}/*")
                    .permitAll()
                    .methods(*GRPC_READS.toTypedArray())
                    .hasAnyAuthority(ROLE_PREFIX + READER, ROLE_PREFIX + WRITER)
                    .methods(*GRPC_WRITES.toTypedArray())
                    .hasAuthority(ROLE_PREFIX + WRITER)
                    .allRequests()
                    .denyAll()
            }.oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(converter) } }
            .build()

    @Bean
    @Profile("web & local")
    fun localSecurity(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            anonymous { principal = LOCAL_USER }
            authorizeHttpRequests { authorize(anyRequest, permitAll) }
        }
        return http.build()
    }

    private fun JsonMapper.writeError(
        response: HttpServletResponse,
        exception: Exception,
    ) {
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        writeValue(response.outputStream, ApiError(exception.javaClass.simpleName, exception.message))
    }
}
