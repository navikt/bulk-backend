package no.nav.bulk.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.*
import no.nav.bulk.generated.pdlquery.Person
import no.nav.bulk.lib.*
import no.nav.bulk.logger
import no.nav.bulk.models.PDLResponse
import no.nav.bulk.models.PeopleDataRequest
import no.nav.bulk.models.PeopleDataResponse
import no.nav.bulk.models.PersonData
import java.lang.Integer.max
import java.lang.Integer.min
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

enum class ResponseFormat {
    JSON, CSV
}

/**
 * Function to get the correct access token (OBO of lient credentials) based on environment.
 * Returns the access token or null if unauthorized or the token was not accessible.
 */
fun getCorrectAccessToken(call: ApplicationCall): String? {
    if (RunEnv.isDevelopment()) return getAccessTokenClientCredentials(AuthConfig.SCOPE)
    val accessToken = call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: return null
    return getAccessTokenOBO(AuthConfig.SCOPE, accessToken)
}

/**
 * This method executes the requests to the DigDir-KRR endpoint, via the getContactinfo-method.
 * Splits the requests into batches of 500 (max limit) until the whole list of pnrs is done. Maps
 * the response into PeopleDataResponse and creates the CSV file, that is used in the response to
 * the call.
 */
// TODO: Make application able to respond to JSON and also include PDL. Create a function to take two responses and merge into one generic map that can be serialized to JSON
suspend fun personerEndpointResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val call = pipelineContext.call
    val responseFormat = if (call.request.queryParameters["type"] == "csv") ResponseFormat.CSV else ResponseFormat.JSON
    val includePdl = call.request.queryParameters["pdl"].toBoolean()
    val accessTokenKRR = getCorrectAccessToken(call) ?: return call.respond(HttpStatusCode.Unauthorized)
    val accessTokenPDL = getAccessTokenClientCredentials(AuthConfig.PDL_API_SCOPE)
        ?: return call.respond(HttpStatusCode.InternalServerError)
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
        "Time Deserialize request data: ${startCallReceive.until(endCallReceive, ChronoUnit.MILLIS)} ms"
    )
    logger.info("Recieved request for ${requestData.personidenter.size} pnrs")

    if (includePdl && responseFormat == ResponseFormat.JSON) call.respond(
        HttpStatusCode.BadRequest,
        "Cannot include PDL data and respond with JSON. Change respond type to CSV or do not include PDL in query parameters."
    )

    val startBatchRequest = LocalDateTime.now()
    lateinit var peopleDataResponse: PeopleDataResponse
    var pdlResponse: PDLResponse? = null

    runBlocking {
        launch {
            peopleDataResponse = constructPeopleDataResponse(requestData.personidenter, accessTokenKRR, navCallId)
        }
        if (includePdl && responseFormat == ResponseFormat.CSV) {
            launch {
                pdlResponse = constructPDLResponse(requestData.personidenter, accessTokenPDL)
            }
        }
    }
    val endBatchRequest = LocalDateTime.now()
    logger.info(
        "Time batch request: ${startBatchRequest.until(endBatchRequest, ChronoUnit.SECONDS)} sec"
    )

    respondCall(call, peopleDataResponse, pdlResponse, responseFormat)
}

suspend fun constructPeopleDataResponse(
    identer: List<String>,
    accessToken: String,
    navCallId: String
): PeopleDataResponse =
    PeopleDataResponse(
        performBulkRequestsInParallel(
            identer,
            accessToken,
            navCallId,
            ::getPeopleDataResponse
        )
    )

suspend fun constructPDLResponse(identer: List<String>, accessToken: String): PDLResponse =
    performBulkRequestsInParallel(
        identer,
        accessToken,
        navCallId = "", // Don't care about navCallId here
        reqFunc = ::getPDLResponse,
    )

suspend fun <Value> performBulkRequestsInParallel(
    identer: List<String>,
    accessToken: String,
    navCallId: String,
    reqFunc: suspend (identer: List<String>, accessToken: String, threadNr: Int, threadBatchSize: Int, navCallId: String) -> Map<String, Value>

): Map<String, Value> {
    val valueMap = mutableMapOf<String, Value>()
    val numThreads = min(max(identer.size / 10_000, 1), 20)
    val batchSizeForThreads = identer.size / numThreads
    val deferredMutableList = mutableListOf<Deferred<Map<String, Value>>>()

    coroutineScope {
        launch {
            for (i in 0 until numThreads) {
                val deferred = async {
                    reqFunc(
                        identer, accessToken, i, batchSizeForThreads, navCallId
                    )
                }
                deferredMutableList.add(deferred)
            }
        }
    }
    for (deferred in deferredMutableList) {
        val value = deferred.await()
        valueMap.putAll(value)
    }
    return valueMap
}

suspend fun getPDLResponse(
    identer: List<String>,
    accessToken: String,
    threadNr: Int,
    batchSize: Int,
    @Suppress("UNUSED_PARAMETER") navCallId: String
): PDLResponse {
    val pdlResponseTotal = mutableMapOf<String, Person?>()
    val stepSize = 100

    for (j in threadNr * batchSize until threadNr * batchSize + batchSize step stepSize) {
        val end = min(j + stepSize, identer.size)
        val pdlResponse = getPDLInfo(
            identer.slice(j until end), accessToken = accessToken
        ) ?: continue
        pdlResponseTotal.putAll(pdlResponse)
    }
    return pdlResponseTotal
}

suspend fun getPeopleDataResponse(
    identer: List<String>, accessToken: String, threadNr: Int, batchSize: Int, navCallId: String,
): Map<String, PersonData> {
    val peopleDataResponse = PeopleDataResponse(mutableMapOf())
    val stepSize = 500

    for (j in threadNr * batchSize until threadNr * batchSize + batchSize step stepSize) {
        val end = min(j + stepSize, identer.size)
        val digDirResponse = getContactInfo(
            identer.slice(j until end), accessToken = accessToken, navCallId = navCallId
        ) ?: continue
        val filteredPeopleInfo = filterAndMapDigDirResponse(digDirResponse)
        (peopleDataResponse.personer as MutableMap).putAll(filteredPeopleInfo.personer)
    }
    return peopleDataResponse.personer
}

suspend fun respondCall(
    call: ApplicationCall,
    peopleDataResponse: PeopleDataResponse,
    pdlResponse: PDLResponse?,
    responseFormat: ResponseFormat
) {
    if (responseFormat == ResponseFormat.CSV) {
        val startMapToCSV = LocalDateTime.now()
        val peopleCSV = mapToCSV(peopleDataResponse, pdlResponse)
        val endMapToCSV = LocalDateTime.now()
        logger.info(
            "Time mapping to CSV: ${startMapToCSV.until(endMapToCSV, ChronoUnit.MILLIS)} ms"
        )
        call.respondText(peopleCSV)
    } else call.respond(peopleDataResponse)
}

fun Application.configureRouting() {
    routing {
        get("/isalive") { call.respond("Alive") }

        get("/isready") { call.respond("Ready") }

        fun postPersonerEndpoint() = post("/personer") {
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
            //val res = getPDLInfo(requestData.personidenter)
            //if (res != null) call.respond(res) else call.respond(500)
        }

        if (!RunEnv.isDevelopment()) authenticate { pdlEndpointRequest() } else pdlEndpointRequest()

        authenticate { get("/auth") { call.respond(HttpStatusCode.OK) } }
    }
}

fun getNavCallId(call: ApplicationCall): String {
    return call.request.headers["Nav-Call-Id"].also { logger.info("Forward Nav-Call-Id: $it") } ?: UUID.randomUUID()
        .toString().also { logger.info("Create new Nav-Call-Id: $it") }
}
