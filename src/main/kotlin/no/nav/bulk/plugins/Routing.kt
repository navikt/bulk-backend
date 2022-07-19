package no.nav.bulk.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import mapToCSV
import no.nav.bulk.lib.RunEnv
import no.nav.bulk.lib.filterAndMapDigDirResponse
import no.nav.bulk.lib.getAccessToken
import no.nav.bulk.lib.getContactInfo
import no.nav.bulk.models.PeopleDataRequest


enum class ResponseFormat {
    JSON,
    CSV
}

suspend fun personerEndpointResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val call = pipelineContext.call
    val requestData: PeopleDataRequest = call.receive()
    val responseFormat =
        if (call.request.queryParameters["type"] == "csv") ResponseFormat.CSV else ResponseFormat.JSON
    val accessToken = getAccessToken() ?: return call.respond(HttpStatusCode.Unauthorized)

    val digDirResponse = getContactInfo(
        requestData.personidenter,
        accessToken = accessToken
    ) ?: return call.respond(HttpStatusCode.InternalServerError)

    val filteredPeopleInfo = filterAndMapDigDirResponse(digDirResponse)
    if (responseFormat == ResponseFormat.CSV) {
        val peopleCSV = mapToCSV(filteredPeopleInfo)
        call.respondText(peopleCSV)
    } else call.respond(filteredPeopleInfo)

}

fun Application.configureRouting() {
    val personerEndpointString = "/personer"

    routing {
        get("/isalive") {
            call.respond("Alive")
        }

        get("/isready") {
            call.respond("Ready")
        }

        // Authenticate user only in production
        if (RunEnv.ENV == "development") {
            post(personerEndpointString) {
                personerEndpointResponse(this)
            }
        } else {
            authenticate {
                post(personerEndpointString) {
                    personerEndpointResponse(this)
                }
            }
        }
    }
}