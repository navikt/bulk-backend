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
import no.nav.bulk.logger
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.PeopleDataRequest
import java.lang.Integer.min
import java.time.LocalDateTime


enum class ResponseFormat {
    JSON,
    CSV
}

suspend fun personerEndpointResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val call = pipelineContext.call
    val requestData: PeopleDataRequest = call.receive()
    // TODO: remove log
    logger.info("Recieved request for ${requestData.personidenter.size} pnrs")
    // TODO: log the requested data, who requested the data, etc. 
    val responseFormat =
        if (call.request.queryParameters["type"] == "csv") ResponseFormat.CSV else ResponseFormat.JSON
    val accessToken = getAccessToken() ?: return call.respond(HttpStatusCode.Unauthorized)

    val digDirResponseTotal = DigDirResponse(mutableMapOf(), mutableMapOf())

    logger.info("Start batch request: ${LocalDateTime.now()}")
    for (i in 0 until requestData.personidenter.size step 500) {
        val end = min(i + 500, requestData.personidenter.size)
        val digDirResponse = getContactInfo(
            requestData.personidenter.slice(i until end),
            accessToken = accessToken
        ) ?: return call.respond(HttpStatusCode.InternalServerError)
        (digDirResponseTotal.personer as MutableMap).putAll(digDirResponse.personer)
        (digDirResponseTotal.feil as MutableMap).putAll(digDirResponse.feil)
    }
    logger.info("Finished batch request: ${LocalDateTime.now()}")
    // At this stage, all the communication with DigDir is done
    logger.info("Size of personer map: ${digDirResponseTotal.personer.size}")

    logger.info("Start filter and map: ${LocalDateTime.now()}")
    val filteredPeopleInfo = filterAndMapDigDirResponse(digDirResponseTotal)
    if (responseFormat == ResponseFormat.CSV) {
        val peopleCSV = mapToCSV(filteredPeopleInfo)
        call.respondText(peopleCSV)
    } else call.respond(filteredPeopleInfo)
    logger.info("Finished filter and map: ${LocalDateTime.now()}")
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
                    logger.info("Inside request for personer")
                    personerEndpointResponse(this)
                }
            }
        }
    }
}