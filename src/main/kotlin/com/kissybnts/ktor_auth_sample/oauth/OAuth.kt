package com.kissybnts.ktor_auth_sample.oauth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kissybnts.ktor_auth_sample.location.Index
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.authentication
import io.ktor.client.HttpClient
import io.ktor.client.bodyStream
import io.ktor.client.call.call
import io.ktor.client.request.header
import io.ktor.client.utils.url
import io.ktor.html.respondHtml
import io.ktor.locations.get
import io.ktor.locations.location
import io.ktor.locations.locations
import io.ktor.locations.oauthAtLocation
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.routing.Route
import io.ktor.routing.param
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.html.title
import java.util.concurrent.Executors
import kotlinx.html.*
import io.ktor.http.HttpMethod
import io.ktor.response.respond
import org.apache.http.HttpHeaders

@location("/oauth/login/{type?}") data class Login(val type: String = "", val code: String? = null, val state: String? = null)

val loginProviders = listOf(
        OAuthServerSettings.OAuth2ServerSettings(
                name = "github",
                authorizeUrl = "https://github.com/login/oauth/authorize",
                accessTokenUrl = "https://github.com/login/oauth/access_token",
                clientId = System.getenv("GITHUB_CLIENT_ID"),
                defaultScopes = listOf("read:user"),
                clientSecret = System.getenv("GITHUB_CLIENT_SECRET")
        )
).associateBy { it.name }

private val exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4)

fun Route.oauth(client: HttpClient) {
    get<Index> {
        call.respondHtml {
            head {
                title { +"index page" }
            }
            body {
                h1 {
                    +"Try to login"
                }
                p {
                    a(href = locations.href(Login())) {
                        +"Login"
                    }
                }
            }
        }
    }

    location<Login> {
        authentication {
            oauthAtLocation<Login>(client, exec.asCoroutineDispatcher(),
                    providerLookup = { loginProviders[it.type] },
                    urlProvider = { _, p -> redirectUrl(Login(p.name), false) })
        }

        param("error") {
            handle {
                call.loginFailedPage(call.parameters.getAll("error").orEmpty())
            }
        }

        intercept(ApplicationCallPipeline.Infrastructure) {
            println("Infrastructure, Intercept! ")
        }
        intercept(ApplicationCallPipeline.Call) {
            println("Call, Intercepted!")
        }
        intercept(ApplicationCallPipeline.Fallback) {
            println("Fallback, Intercepted!")
        }

        handle {
            val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
            if (principal != null) {
                val state = call.parameters["state"]
                val code = call.parameters["code"]
                val req = client.call {
                    header(HttpHeaders.AUTHORIZATION, "Bearer ${principal.accessToken}")
                    url("https", "api.github.com", 443, "user")
                    method = HttpMethod.Get
                }

                val user = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE).readValue(req.bodyStream.reader(Charsets.UTF_8), GithubUser::class.java)

                call.respond(user)
//                call.loggedInSuccessResponse(principal)
            } else {
                call.loginPage()
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubUser(val login: String, val id: Int, val avatarUrl: String, val name: String)


private fun <T : Any> ApplicationCall.redirectUrl(t: T, secure: Boolean = true): String {
    val hostPort = request.host()!! + request.port().let { port -> if (port == 80) "" else ":$port" }
    val protocol = when {
        secure -> "https"
        else -> "http"
    }
    return "$protocol://$hostPort${application.locations.href(t)}"
}

private suspend fun ApplicationCall.loginPage() {
    respondHtml {
        head {
            title { +"Login with" }
        }
        body {
            h1 {
                +"Login with:"
            }
            loginProviders.forEach {
                p {
                    a(href = application.locations.href(Login(it.key))) {
                        +it.key
                    }
                }
            }
        }
    }
}

private suspend fun ApplicationCall.loggedInSuccessResponse(callback: OAuthAccessTokenResponse.OAuth2) {
    respondHtml {
        head {
            title { +"You are logged in" }
        }
        body {
            h1 {
                +"You are logged in"
            }
            p {
                +"Your token is ${callback.accessToken}"
            }
            p {
                +"Token type is ${callback.tokenType}"
            }
            p {
                +"Refresh token is ${callback.refreshToken}"
            }
            p {
                +"Extra parameters are ${callback.extraParameters}"
            }
        }
    }
}


private suspend fun ApplicationCall.loginFailedPage(errors: List<String>) {
    respondHtml {
        head {
            title { +"Login with" }
        }
        body {
            h1 {
                +"Login error"
            }

            for (e in errors) {
                p {
                    +e
                }
            }
        }
    }
}