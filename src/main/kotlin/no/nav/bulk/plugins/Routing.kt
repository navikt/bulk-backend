package no.nav.bulk.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.bulk.lib.*
import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataRequest
import no.nav.bulk.models.PeopleDataResponse
import java.lang.Integer.min
import java.time.LocalDateTime


enum class ResponseFormat {
    JSON,
    CSV
}

/**
 * This method executes the requests to the DigDir-KRR endpoint, via the getContactinfo-method.
 * Splits the requests into batches of 500 (max limit) until the whole list of pnrs is done.
 * Maps the response into PeopleDataResponse and creates the CSV file, that is used in the response to the call.
 */
suspend fun personerEndpointResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val call = pipelineContext.call
    val requestData: PeopleDataRequest
    try {
        requestData = call.receive()
    } catch (e: CannotTransformContentToTypeException) {
        return call.respond(HttpStatusCode.BadRequest)
    }

    // TODO: remove log
    logger.info("Recieved request for ${requestData.personidenter.size} pnrs")
    // TODO: log the requested data, who requested the data, etc. 
    val responseFormat =
        if (call.request.queryParameters["type"] == "csv") ResponseFormat.CSV else ResponseFormat.JSON
    val accessToken = getAccessToken() ?: return call.respond(HttpStatusCode.Unauthorized)

    val peopleDataResponseTotal = PeopleDataResponse(mutableMapOf())

    logger.info("Start batch request: ${LocalDateTime.now()}")
    for (i in 0 until requestData.personidenter.size step 500) {
        val end = min(i + 500, requestData.personidenter.size)
        val digDirResponse = getContactInfo(
            requestData.personidenter.slice(i until end),
            accessToken = accessToken
        ) ?: return call.respond(HttpStatusCode.InternalServerError)
        val filteredPeopleInfo = filterAndMapDigDirResponse(digDirResponse)
        (peopleDataResponseTotal.personer as MutableMap).putAll(filteredPeopleInfo.personer)
    }
    logger.info("Finished batch request: ${LocalDateTime.now()}")
    // At this stage, all the communication with DigDir is done
    logger.info("Size of personer map: ${peopleDataResponseTotal.personer.size}")

    if (responseFormat == ResponseFormat.CSV) {
        val peopleCSV = mapToCSV(peopleDataResponseTotal)
        call.respondText(peopleCSV)
    } else call.respond(peopleDataResponseTotal)
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