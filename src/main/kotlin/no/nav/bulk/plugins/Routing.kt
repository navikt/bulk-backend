package no.nav.bulk.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.bulk.lib.filterAndMapDigDirResponse
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
            if (res != null) {
                val filteredPeopleInfo = filterAndMapDigDirResponse(res)
                call.respond(filteredPeopleInfo)
            } else
                call.respond(HttpStatusCode.InternalServerError)
        }
        route("/auth") {
            authenticate {
                get("/test") {
                    call.request.headers.entries().forEach { println("${it.key} : ${it.value}") } 
                    call.respond("Authenticated")
                }
            }
        }
    }
}
