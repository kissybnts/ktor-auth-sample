package com.kissybnts.ktor_auth_sample.basic

import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.locations.location
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.decodeBase64

@location("/manual") class Manual

@location("/userTable") class SimpleUserTable

val hashedUserTable = UserHashedTableAuth(table = mapOf("test" to decodeBase64("VltM4nfheqcJSyH887H+4NEOm2tDuKCl83p5axYXlF0=")))

fun Route.basicAuth() {
    location<Manual> {
        authentication {
            basicAuthentication("ktor") { credential: UserPasswordCredential ->
                println("name = ${credential.name}, password = ${credential.password}")
                if (credential.name == credential.password) {
                    UserIdPrincipal(credential.name)
                } else {
                    null
                }
            }
        }

        get {
            call.respondText("Success, ${call.principal<UserIdPrincipal>()?.name}")
        }
    }

    location<SimpleUserTable> {
        authentication {
            basicAuthentication("ktor") { credential: UserPasswordCredential ->
                hashedUserTable.authenticate(credential)
            }
        }

        get {
            call.respond("Success, ${call.principal<UserIdPrincipal>()?.name}")
        }
    }
}

fun a() {

}