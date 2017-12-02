package com.kissybnts.ktor_auth_sample.post

import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.html.respondHtml
import io.ktor.http.HttpMethod
import io.ktor.locations.location
import io.ktor.request.path
import io.ktor.request.uri
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.method
import kotlinx.html.*

@location("/form-post") class FormPost(val error: String? = null)

data class FormPostUser(val name: String, val password: String)

val userList = listOf(
        FormPostUser("foo", "bar"),
        FormPostUser("piyo", "piyo")
)

fun Route.formPost() {
    location<FormPost> {
        get {
            val error = call.parameters["error"]
            call.respondHtml {
                body {
                    form(action = "/form-post", method = FormMethod.post) {
                        if (error != null) {
                            p {
                                +"Error: $error"
                            }
                        }
                        p {
                            +"User name:"
                            textInput(name = "name") { value = "" }
                        }
                        p {
                            +"Password: "
                            passwordInput(name = "password") { value = "" }
                        }
                        p {
                            submitInput { value = "Login" }
                        }
                    }
                }
            }
        }

        method(HttpMethod.Post) {
            authentication {
                formAuthentication(userParamName = "name", challenge = FormAuthChallenge.Redirect { call, _ ->  call.request.uri + "?error=Invalid credential" }) { userPasswordCredential: UserPasswordCredential ->
                    val requestUser = FormPostUser(userPasswordCredential.name, userPasswordCredential.password)
                    userList.find { it == requestUser }?.let { UserIdPrincipal(it.name) }
                }
            }

            handle {
                val principal = call.principal<UserIdPrincipal>()?: throw IllegalStateException("Principal is null.")

                call.respondHtml {
                    body {
                        h1 {
                            +"Hello, ${principal.name}"
                        }
                    }
                }
            }
        }
    }
}