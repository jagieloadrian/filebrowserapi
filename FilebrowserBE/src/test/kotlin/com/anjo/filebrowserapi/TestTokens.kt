package com.anjo.filebrowserapi

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import org.springframework.test.context.DynamicPropertyRegistry
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date

object TestTokens {
    const val AUDIENCE = "filebrowser"
    const val READER = "filebrowser-reader"
    const val WRITER = "filebrowser-writer"

    private val keys = newKeyPair()

    val issuer: String by lazy {
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        val issuer = "http://127.0.0.1:${server.address.port}"
        val jwks = JWKSet(RSAKey.Builder(keys.public as RSAPublicKey).keyID("test").build()).toString()
        server.serveJson("/.well-known/openid-configuration", """{"issuer":"$issuer","jwks_uri":"$issuer/jwks"}""")
        server.serveJson("/jwks", jwks)
        server.start()
        issuer
    }

    fun register(registry: DynamicPropertyRegistry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { issuer }
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences") { AUDIENCE }
    }

    fun token(
        vararg groups: String,
        issuer: String = this.issuer,
        audience: String = AUDIENCE,
        expiresAt: Instant = Instant.now().plusSeconds(300),
        signingKey: PrivateKey = keys.private,
        subject: String = "user-1",
    ): String {
        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(subject)
                .claim("groups", groups.toList())
                .issueTime(Date())
                .expirationTime(Date.from(expiresAt))
                .build()
        return SignedJWT(JWSHeader(JWSAlgorithm.RS256), claims).apply { sign(RSASSASigner(signingKey)) }.serialize()
    }

    fun foreignKey(): PrivateKey = newKeyPair().private

    private fun HttpServer.serveJson(
        path: String,
        body: String,
    ) = createContext(path) { exchange ->
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
}
