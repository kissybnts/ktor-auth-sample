package com.kissybnts.ktor_auth_sample

import com.fasterxml.jackson.databind.SerializationFeature
import com.kissybnts.ktor_auth_sample.location.Index
import com.kissybnts.ktor_auth_sample.location.ResourceId
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
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

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Locations)

    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.INDENT_OUTPUT, true)
        }
    }

    install(Routing) {
        get<Index> {
            call.respond("Hello  world from Ktor!")
        }
        route("/resources") {
            get<ResourceId> {
                val item = Item(it.id, "Item ${it.id}", LocalDateTime.now())
                call.respond(item)
            }
        }
    }
}

data class Item(val id: Int, val name: String, val date: LocalDateTime)