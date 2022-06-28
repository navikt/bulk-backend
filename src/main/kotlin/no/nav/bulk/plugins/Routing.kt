package no.nav.bulk.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import no.nav.bulk.models.Hello

fun Application.configureRouting() {

    routing {
        get("/") {
            //call.respondText("Hello World!")
            call.respond<Hello>(Hello("Hello World!!!!"))

        }

        get("/isalive") {
            call.respond("Alive")
        }

        get("/isready") {
            call.respond("Ready")
        }
    }
}
