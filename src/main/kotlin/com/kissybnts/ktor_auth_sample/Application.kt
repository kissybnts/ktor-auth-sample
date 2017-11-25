package com.kissybnts.ktor_auth_sample

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.kissybnts.ktor_auth_sample.basic.basicAuth
import com.kissybnts.ktor_auth_sample.location.Index
import com.kissybnts.ktor_auth_sample.location.ResourceId
import com.kissybnts.ktor_auth_sample.oauth.oauth
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
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import java.time.LocalDateTime
import java.util.*

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
    }
}