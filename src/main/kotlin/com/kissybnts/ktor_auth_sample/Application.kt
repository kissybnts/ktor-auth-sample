package com.kissybnts.ktor_auth_sample

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.kissybnts.ktor_auth_sample.basic.basicAuth
import com.kissybnts.ktor_auth_sample.jwt.AuthenticationProvider
import com.kissybnts.ktor_auth_sample.jwt.User
import com.kissybnts.ktor_auth_sample.jwt.generateToken
import com.kissybnts.ktor_auth_sample.jwt.jwt
import com.kissybnts.ktor_auth_sample.oauth.oauth
import com.kissybnts.ktor_auth_sample.post.formPost
import com.kissybnts.ktor_auth_sample.util.util
import io.ktor.application.Application
import io.ktor.application.ApplicationStopping
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.backend.apache.ApacheBackend
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Locations)

    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.INDENT_OUTPUT, true)
            propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        }
    }

    install(Routing) {
        util()
        route("/basic") {
            basicAuth()
        }

        val client = HttpClient(ApacheBackend)
        environment.monitor.subscribe(ApplicationStopping) {
            client.close()
        }
        oauth(client)

        get("/jwt") {
            call.respond(generateToken(User(1, "name", AuthenticationProvider.GITHUB.providerId, "hogehoge")))
        }

        jwt(client)

        formPost()
    }
}