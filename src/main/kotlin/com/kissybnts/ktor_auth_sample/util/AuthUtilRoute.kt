package com.kissybnts.ktor_auth_sample.util

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.AuthenticationPipeline
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.response.HttpResponsePipeline
import io.ktor.locations.get
import io.ktor.locations.location
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import java.util.*

@location("/basic") data class UserNamePassword(val name: String, val password: String)


fun Route.util() {
    route("/util") {
        get<UserNamePassword> {
            val encoded = Base64.getEncoder().encode("${it.name}:${it.password}".toByteArray(Charsets.ISO_8859_1)).toString(Charsets.ISO_8859_1)
            call.respond("Basic $encoded")
        }
    }

    get("/phases") {
        val send = application.sendPipeline.items.map { it.name }
        val receive = application.receivePipeline.items.map { it.name }
        val applicationCall = listOf<String>(ApplicationCallPipeline.Infrastructure.name, ApplicationCallPipeline.Call.name, ApplicationCallPipeline.Fallback.name)

        call.respond(mapOf<String, List<String>>("receive" to receive, "send" to send, "applicationCall" to applicationCall))
    }

    route("/intercept") {
        intercept(ApplicationCallPipeline.Infrastructure) {
            println("Infrastructure, Intercepted!")
        }
        intercept(ApplicationCallPipeline.Call) {
            println("Call, Intercepted!")
        }
        intercept(ApplicationCallPipeline.Fallback) {
            println("Fallback, Intercepted!")
        }
        get {
            println("get")
            call.respond("OK")
        }
    }
}