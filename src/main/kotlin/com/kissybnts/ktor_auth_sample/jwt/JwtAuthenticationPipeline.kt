package com.kissybnts.ktor_auth_sample.jwt

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kissybnts.ktor_auth_sample.oauth.GithubUser
import com.kissybnts.ktor_auth_sample.oauth.loginProviders
import com.kissybnts.ktor_auth_sample.oauth.redirectUrl
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SignatureException
import io.jsonwebtoken.impl.crypto.MacProvider
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.bodyStream
import io.ktor.client.call.call
import io.ktor.client.request.header
import io.ktor.client.utils.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.location
import io.ktor.locations.oauthAtLocation
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.method
import io.ktor.routing.param
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Executors

@location("/api-jwt") class JwtApi
@location("/jwt/login/{type}") data class JwtLogin(val type: String, val code: String? = null, val state: String? = null)

private val exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4)

data class LoginResponse(val user: User, val token: String)

fun Route.jwt(client: HttpClient) {
    location<JwtLogin> {
        authentication {
            oauthAtLocation<JwtLogin>(client,
                    exec.asCoroutineDispatcher(),
                    providerLookup = { loginProviders[it.type] },
                    urlProvider = { _, p -> redirectUrl(JwtLogin(p.name), false)})
        }

        param("error") {
            handle {
                call.respond(HttpStatusCode.BadRequest, call.parameters.getAll("error").orEmpty())
            }
        }

        handle {
            val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
            if (principal != null) {
                val code = call.parameters["code"]

                if (code == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@handle
                }

                val getUserRequest = client.call {
                    header(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                    url("https", "api.github.com", 443, "user")
                    method = HttpMethod.Get
                }

                val gitHubUser = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE).readValue(getUserRequest.bodyStream.reader(Charsets.UTF_8), GithubUser::class.java)

                val user = User(1, gitHubUser.name, gitHubUser.id, code)

                val token = generateToken(user)

                call.respond(LoginResponse(user, token))
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }

    location<JwtApi> {
        authentication {
            jwtAuthentication("Ktor-auth")
        }

        method(HttpMethod.Get) {
            handle {
                val user = call.principal<User>()

                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Token is invalid.")
                }
            }
        }
    }
}

fun AuthenticationPipeline.jwtAuthentication(realm: String) {
    intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val authHeader = call.request.parseAuthorizationHeader()

        val user: User? = if (authHeader?.authScheme == "Bearer" && authHeader is HttpAuthHeader.Single) {
            try {
                verifyJwtToken(authHeader.blob)
            } catch (ex: Exception) {
                println(ex.message)
                null
            }
        } else {
            null
        }

        if (user != null) {
            context.principal(user)
        } else {
            context.challenge("JwtAuth", NotAuthenticatedCause.InvalidCredentials) {
                it.success()
                call.respond(UnauthorizedResponse(HttpAuthHeader.basicAuthChallenge(realm)))
            }
        }
    }
}

val aud = "Ktor-auth"

// TODO replace to secret key gotten from somewhere
val key = MacProvider.generateKey()

fun generateToken(user: User): String {
    val expiration = LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault())
    return Jwts.builder()
            .setSubject(jacksonObjectMapper().writeValueAsString(user))
            .setAudience(aud)
            .signWith(SignatureAlgorithm.HS512, key)
            .setExpiration(Date.from(expiration.toInstant()))
            .setHeaderParam("typ", "JWT")
            .compact()
}

fun verifyJwtToken(token: String): User {
    val jws = try {
        Jwts.parser().setSigningKey(key).parseClaimsJws(token)
    } catch (ex: SignatureException) {
        throw Exception("Invalid token")
    }

    val now = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())

    if (jws.body.expiration.before(now)) {
        throw Exception("Token has already been expired.")
    }

    val sub = jws.body.subject ?: throw Exception("Subject is null")

    return try {
        jacksonObjectMapper().readValue(sub, User::class.java)
    } catch (e: Exception) {
        throw Exception("Subject is not serializable to User class.")
    }
}

enum class AuthenticationProvider(val providerId: Int, val providerName: String) {
    GITHUB(1, "github")
}

data class User(val id: Int, val name: String, val provider: Int, val providerCode: String): Principal