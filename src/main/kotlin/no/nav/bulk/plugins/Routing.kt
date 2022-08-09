package no.nav.bulk.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import no.nav.bulk.generated.pdlquery.Person
import no.nav.bulk.lib.*
import no.nav.bulk.logger
import no.nav.bulk.models.*
import java.lang.Integer.max
import java.lang.Integer.min
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

enum class ResponseFormat {
    JSON, CSV
}

/**
 * Function to get the correct access token (OBO of client credentials) based on environment.
 * Returns the access token or null if unauthorized or the token was not accessible.
 */
fun getCorrectAccessToken(headers: Headers): String? {
    if (RunEnv.isDevelopment()) return getAccessTokenClientCredentials(AuthConfig.KRR_API_SCOPE)
    val accessToken = headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: return null
    return getAccessTokenOBO(AuthConfig.KRR_API_SCOPE, accessToken)
}

fun combineKRRAndPDL(peopleDataResponse: PeopleDataResponse, pdlResponse: PDLResponse?): PersonerResponse {
    val personerResponseMap = mutableMapOf<String, PersonResponse>()
    for ((personident, personData) in peopleDataResponse.personer) {
        val pdlPerson = pdlResponse?.getValue(personident)
        val personTotal = PersonTotal(
            personident = personident,
            spraak = personData.person?.spraak,
            epostadresse = personData.person?.epostadresse,
            mobiltelefonnummer = personData.person?.mobiltelefonnummer,
            adresse = personData.person?.adresse,
            navn = pdlPerson?.navn,
            bostedsadresse = pdlPerson?.bostedsadresse,
            doedsfall = pdlPerson?.doedsfall
        )
        val persData = PersonResponse(personTotal, personData.feil)
        personerResponseMap[personident] = persData
    }
    return PersonerResponse(personerResponseMap)
}

sealed class PersonerStatus {
    class Error(val status: HttpStatusCode, val message: String? = null) : PersonerStatus()
    class SuccessCSV(val csv: String) : PersonerStatus()
    class SuccessJson(val response: PersonerResponse) : PersonerStatus()

}

/**
 * This method executes the requests to the DigDir-KRR endpoint, via the getContactinfo-method.
 * Splits the requests into batches of 500 (max limit) until the whole list of pnrs is done. Maps
 * the response into PeopleDataResponse and creates the CSV file, that is used in the response to
 * the call.
 */
suspend fun personerEndpointResponse(
    headers: Headers,
    queryParameters: Parameters,
    requestData: PeopleDataRequest
): PersonerStatus {
    val responseFormat = if (queryParameters["type"] == "csv") ResponseFormat.CSV else ResponseFormat.JSON
    val includePdl = queryParameters["pdl"].toBoolean()
    val accessTokenKRR = getCorrectAccessToken(headers) ?: return PersonerStatus.Error(HttpStatusCode.Unauthorized)
    val accessTokenPDL = getAccessTokenClientCredentials(AuthConfig.PDL_API_SCOPE)
        ?: return PersonerStatus.Error(HttpStatusCode.InternalServerError)
    val navCallId = getNavCallId(headers)

    val startBatchRequest = LocalDateTime.now()
    lateinit var peopleDataResponse: PeopleDataResponse
    var pdlResponse: PDLResponse? = null

    runBlocking {
        launch {
            peopleDataResponse = constructPeopleDataResponse(requestData.personidenter, accessTokenKRR, navCallId)
        }
        if (includePdl) {
            launch {
                pdlResponse = constructPDLResponse(requestData.personidenter, accessTokenPDL)
            }
        }
    }
    val endBatchRequest = LocalDateTime.now()
    logger.info(
        "Time batch request: ${startBatchRequest.until(endBatchRequest, ChronoUnit.SECONDS)} sec"
    )
    if (peopleDataResponse.personer.isEmpty()) return PersonerStatus.Error(
        HttpStatusCode.InternalServerError,
        "KRR responded with no data."
    )
    if (responseFormat == ResponseFormat.CSV)
        return PersonerStatus.SuccessCSV(mapToCSV(peopleDataResponse, pdlResponse))
    return PersonerStatus.SuccessJson(combineKRRAndPDL(peopleDataResponse, pdlResponse))
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
    println("Finshed bulk request")
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

fun Application.configureRouting() {
    routing {
        get("/isalive") { call.respond("Alive") }

        get("/isready") { call.respond("Ready") }

        fun postPersonerEndpoint() = post("/personer") {
            logger.info("Process request")
            val start = LocalDateTime.now()
            val requestData: PeopleDataRequest
            try {
                requestData = call.receive()
            } catch (e: CannotTransformContentToTypeException) {
                return@post call.respond(HttpStatusCode.BadRequest, e.message.toString())
            }
            when (val status =
                personerEndpointResponse(call.request.headers, call.request.queryParameters, requestData)) {
                is PersonerStatus.Error -> call.respond(status.status, status.message ?: "")
                is PersonerStatus.SuccessCSV -> call.respondText(status.csv)
                is PersonerStatus.SuccessJson -> call.respond(status.response)
            }
            val end = LocalDateTime.now()
            logger.info("Time processing request: ${start.until(end, ChronoUnit.SECONDS)}s")
        }

        if (!RunEnv.isDevelopment()) authenticate { postPersonerEndpoint() }
        else postPersonerEndpoint()

        authenticate { get("/auth") { call.respond(HttpStatusCode.OK) } }
    }
}

fun getNavCallId(headers: Headers): String {
    return headers["Nav-Call-Id"].also { logger.info("Forward Nav-Call-Id: $it") }
        ?: UUID.randomUUID().toString().also { logger.info("Create new Nav-Call-Id: $it") }
}
