package com.kissybnts.ktor_auth_sample.jwt

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.response.respond
import java.net.URL
import java.security.interfaces.RSAPublicKey

val JWTAuthKey = "JwtAuth"

fun AuthenticationPipeline.bearerAuthentication(realm: String) {
    intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val authHeader = call.request.parseAuthorizationHeader()
        val jwt: DecodedJWT? = if (authHeader?.authScheme == "Bearer" && authHeader is HttpAuthHeader.Single) {
            try {
                verifyToken(authHeader.blob)
            } catch (ex: Exception) {
                null
            }
        } else {
            null
        }

        // Transform the token to principal
        val principal = jwt?.let { UserIdPrincipal(jwt.subject ?: jwt.getClaim("client_id").asString()) }

        if (principal != null) {
            context.principal(principal)
        } else {
            context.challenge(JWTAuthKey, NotAuthenticatedCause.InvalidCredentials) {
                it.success()
                call.respond(UnauthorizedResponse(HttpAuthHeader.bearerAuthChallenge(realm)))
            }
        }
    }
}

private fun verifyToken(token: String): DecodedJWT {
    val jwkProvider = UrlJwkProvider(URL("http://localhost:9000/.well-known/openid-configuration/jwks"))

    val jwt = JWT.decode(token)
    val jwk = jwkProvider.get(jwt.keyId)

    val publicKey = jwk.publicKey as? RSAPublicKey ?: throw  Exception("Invalid token type")

    val algorithm = when (jwk.algorithm) {
        "RSA256" -> Algorithm.RSA256(publicKey, null)
        else -> throw Exception("Invalid token algorithm")
    }

    val verifier = JWT.require(algorithm).withIssuer("http://localhost:9000").withAudience("api1").build()

    return verifier.verify(token)
}

private fun HttpAuthHeader.Companion.bearerAuthChallenge(realm: String): HttpAuthHeader = HttpAuthHeader.Parameterized("Bearer", mapOf(HttpAuthHeader.Parameters.Realm to realm))