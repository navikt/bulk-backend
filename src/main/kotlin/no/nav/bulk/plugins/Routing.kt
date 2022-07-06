package no.nav.bulk.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.bulk.lib.getContactInfo
import no.nav.bulk.models.PeopleDataRequest

fun Application.configureRouting() {

    routing {
        get("/isalive") {
            call.respond("Alive")
        }

        get("/isready") {
            call.respond("Ready")
        }

        post("/personer") {
            val requestData = call.receive<PeopleDataRequest>()
            val res = getContactInfo(requestData.personidenter)
            // Add filter here
            call.respond(res)
        }
    }
}
