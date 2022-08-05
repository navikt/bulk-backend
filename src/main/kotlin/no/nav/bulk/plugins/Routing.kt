package no.nav.bulk.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import no.nav.bulk.lib.*
import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataRequest
import no.nav.bulk.models.PeopleDataResponse
import java.lang.Integer.max
import java.lang.Integer.min
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

enum class ResponseFormat {
    JSON,
    CSV
}

/**
 * Function to get the correct access token (OBO of client credentials) based on environment.
 * Returns the access token or null if unauthorized or the token was not accessible.
 */
fun getCorrectAccessToken(call: ApplicationCall): String? {
    if (RunEnv.isDevelopment()) return getAccessTokenClientCredentials(AuthConfig.SCOPE)
    val accessToken =
        call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: return null
    return getAccessTokenOBO(AuthConfig.SCOPE, accessToken)
}

/**
 * This method executes the requests to the DigDir-KRR endpoint, via the getContactinfo-method.
 * Splits the requests into batches of 500 (max limit) until the whole list of pnrs is done. Maps
 * the response into PeopleDataResponse and creates the CSV file, that is used in the response to
 * the call.
 */
suspend fun personerEndpointResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val call = pipelineContext.call
    val accessToken =
        getCorrectAccessToken(call) ?: return call.respond(HttpStatusCode.Unauthorized)
    val navCallId = getNavCallId(call)

    val requestData: PeopleDataRequest
    val startCallReceive = LocalDateTime.now()
    try {
        requestData = call.receive()
    } catch (e: CannotTransformContentToTypeException) {
        return call.respond(HttpStatusCode.BadRequest)
    }
    val endCallReceive = LocalDateTime.now()
    logger.info(
        "Time Deserialize request data: ${startCallReceive.until(endCallReceive, ChronoUnit.MILLIS)}ms"
    )

    logger.info("Recieved request for ${requestData.personidenter.size} pnrs")

    val responseFormat =
        if (call.request.queryParameters["type"] == "csv") ResponseFormat.CSV
        else ResponseFormat.JSON
    val startBatchRequest = LocalDateTime.now()
    val peopleDataResponse = constructPeopleDataResponse(requestData, accessToken, navCallId)
    val endBatchRequest = LocalDateTime.now()
    logger.info(
        "Time batch request: ${startBatchRequest.until(endBatchRequest, ChronoUnit.SECONDS)} sec"
    )

    // At this stage, all the communication with DigDir is done
    respondCall(call, peopleDataResponse, responseFormat)
}

suspend fun constructPeopleDataResponse(
    requestData: PeopleDataRequest,
    accessToken: String,
    navCallId: String,
): PeopleDataResponse {
    val peopleDataResponseTotal = PeopleDataResponse(mutableMapOf())
    val numThreads = min(max(requestData.personidenter.size / 10_000, 1), 20)
    val batchSizeForThreads = requestData.personidenter.size / numThreads
    val deferredMutableList = mutableListOf<Deferred<PeopleDataResponse>>()

    coroutineScope {
        launch {
            for (i in 0 until numThreads) {
                val deferred = async {
                    getPeopleDataResponse(
                        requestData,
                        accessToken,
                        navCallId,
                        i,
                        batchSizeForThreads
                    )
                }
                deferredMutableList.add(deferred)
            }
        }
    }
    for (deferred in deferredMutableList) {
        val peopleDataResponse = deferred.await()
        (peopleDataResponseTotal.personer as MutableMap).putAll(peopleDataResponse.personer)
    }
    return peopleDataResponseTotal
}

suspend fun getPeopleDataResponse(
    requestData: PeopleDataRequest,
    accessToken: String,
    navCallId: String,
    threadId: Int,
    batchSize: Int
): PeopleDataResponse {
    val peopleDataResponse = PeopleDataResponse(mutableMapOf())
    val stepSize = 500

    for (j in threadId * batchSize until threadId * batchSize + batchSize step stepSize) {
        val end = min(j + stepSize, requestData.personidenter.size)
        val digDirResponse = getContactInfo(
            requestData.personidenter.slice(j until end),
            accessToken = accessToken,
            navCallId = navCallId
        ) ?: continue
        val filteredPeopleInfo = filterAndMapDigDirResponse(digDirResponse)
        (peopleDataResponse.personer as MutableMap).putAll(filteredPeopleInfo.personer)
    }
    return peopleDataResponse
}

suspend fun respondCall(
    call: ApplicationCall,
    peopleDataResponse: PeopleDataResponse,
    responseFormat: ResponseFormat
) {
    if (responseFormat == ResponseFormat.CSV) {
        val startMapToCSV = LocalDateTime.now()
        val peopleCSV = mapToCSV(peopleDataResponse)
        val endMapToCSV = LocalDateTime.now()
        logger.info(
            "Time mapping to CSV: ${startMapToCSV.until(endMapToCSV, ChronoUnit.MILLIS)} ms"
        )
        call.respondText(peopleCSV)
    } else call.respond(peopleDataResponse)
}

fun Application.configureRouting() {
    val personerEndpointString = "/personer"

    routing {
        get("/isalive") { call.respond("Alive") }

        get("/isready") { call.respond("Ready") }

        fun postPersonerEndpoint() =
            post(personerEndpointString) {
                logger.info("Process request")
                val start = LocalDateTime.now()
                personerEndpointResponse(this)
                val end = LocalDateTime.now()
                logger.info("Time processing request: ${start.until(end, ChronoUnit.SECONDS)}s")
            }

        if (!RunEnv.isDevelopment()) authenticate { postPersonerEndpoint() }
        else postPersonerEndpoint()

        fun pdlEndpointRequest() = post("/pdl") {

            val requestData: PeopleDataRequest
            try {
                requestData = call.receive()
            } catch (e: CannotTransformContentToTypeException) {
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            val res = getPnrsNames(requestData.personidenter)
            if (res != null) call.respond(res) else call.respond(500)
        }

        if (!RunEnv.isDevelopment()) authenticate { pdlEndpointRequest() } else pdlEndpointRequest()

        authenticate { get("/auth") { call.respond(HttpStatusCode.OK) } }
    }
}

fun getNavCallId(call: ApplicationCall): String {
    return call.request.headers["Nav-Call-Id"].also { logger.info("Forward Nav-Call-Id: $it") }
        ?: UUID.randomUUID().toString().also { logger.info("Create new Nav-Call-Id: $it") }
}
