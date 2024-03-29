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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

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

fun combineKRRAndPDL(peopleDataResponse: MappedKRRResponse, pdlResponse: PDLResponse?): PersonerEndpointResponse {
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
    return PersonerEndpointResponse(personerResponseMap)
}

sealed class PersonerStatus {
    class Error(val status: HttpStatusCode, val message: String? = null) : PersonerStatus()
    class SuccessCSV(val csv: String) : PersonerStatus()
    class SuccessJson(val response: PersonerEndpointResponse) : PersonerStatus()

}

/**
 * This method executes the requests to the DigDir-KRR endpoint, via the getContactinfo-method.
 * Splits the requests into batches of 500 (max limit) until the whole list of pnrs is done. Maps
 * the response into PeopleDataResponse and creates the CSV file, that is used in the response to
 * the call.
 */
suspend fun getPersonerTotalBulk(
    requestData: PersonerEndpointRequest,
    responseFormat: ResponseFormat,
    includePdl: Boolean = false,
    headers: Headers,
): PersonerStatus {
    val accessTokenKRR = getCorrectAccessToken(headers) ?: return PersonerStatus.Error(HttpStatusCode.Unauthorized)
    val accessTokenPDL = getAccessTokenClientCredentials(AuthConfig.PDL_API_SCOPE)
        ?: return PersonerStatus.Error(HttpStatusCode.InternalServerError)
    val navCallId = getNavCallId(headers)

    logger.info("Received request for ${requestData.personidenter.size} personidenter.")
    val startBatchRequest = LocalDateTime.now()
    lateinit var krrResponse: MappedKRRResponse
    var pdlResponse: PDLResponse? = null

    runBlocking {
        launch {
            krrResponse = getMappedKRRDataBulk(requestData.personidenter, accessTokenKRR, navCallId)
        }
        if (includePdl) {
            launch {
                pdlResponse = getPDLDataBulk(requestData.personidenter, accessTokenPDL)
            }
        }
    }
    val endBatchRequest = LocalDateTime.now()
    logger.info(
        "Time batch request: ${startBatchRequest.until(endBatchRequest, ChronoUnit.SECONDS)} sec"
    )
    if (krrResponse.personer.isEmpty()) return PersonerStatus.Error(
        HttpStatusCode.InternalServerError,
        "KRR responded with no data."
    )
    if (responseFormat == ResponseFormat.CSV)
        return PersonerStatus.SuccessCSV(mapToCSV(krrResponse, pdlResponse))
    return PersonerStatus.SuccessJson(combineKRRAndPDL(krrResponse, pdlResponse))
}

fun getOptimalNumberOfThreads(requestSize: Int, batchSizePerRequest: Int, maxNumberOfThreads: Int = 20): Int {
    return min(max(requestSize / batchSizePerRequest * 10, 1), maxNumberOfThreads)
}

suspend fun getMappedKRRDataBulk(
    identer: List<String>,
    accessToken: String,
    navCallId: String
): MappedKRRResponse =
    MappedKRRResponse(
        getBulkFromAPIInParallel(
            identer,
            accessToken,
            navCallId,
            numThreads = getOptimalNumberOfThreads(identer.size, 500),
            ::getMappedKRRRDataAsThread
        )
    )

suspend fun getPDLDataBulk(identer: List<String>, accessToken: String): PDLResponse =
    getBulkFromAPIInParallel(
        identer,
        accessToken,
        navCallId = "", // Don't care about navCallId here
        numThreads = getOptimalNumberOfThreads(identer.size, 100),
        requestPerThreadFunc = ::getPDLDataAsThread,
    )

suspend fun <Value> getBulkFromAPIInParallel(
    identer: List<String>,
    accessToken: String,
    navCallId: String,
    numThreads: Int = 5,
    requestPerThreadFunc: suspend (identer: List<String>, accessToken: String, threadNr: Int, threadBatchSize: Int, navCallId: String) -> Map<String, Value>

): Map<String, Value> {
    val valueMap = mutableMapOf<String, Value>()
    // val numThreads = min(max(identer.size / 10_000, 1), 20)
    val batchSizeForThreads = identer.size / numThreads
    val deferredMutableList = mutableListOf<Deferred<Map<String, Value>>>()

    coroutineScope {
        launch {
            for (i in 0 until numThreads) {
                val deferred = async {
                    requestPerThreadFunc(
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

suspend fun getPDLDataAsThread(
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
        val pdlResponse = getPeopleDataFromPDL(
            identer.slice(j until end), accessToken = accessToken
        ) ?: continue
        pdlResponseTotal.putAll(pdlResponse)
    }
    return pdlResponseTotal
}

suspend fun getMappedKRRRDataAsThread(
    identer: List<String>, accessToken: String, threadNr: Int, batchSize: Int, navCallId: String,
): Map<String, MappedKRRPersonResponse> {
    val peopleDataResponse = MappedKRRResponse(mutableMapOf())
    val stepSize = 500

    for (j in threadNr * batchSize until threadNr * batchSize + batchSize step stepSize) {
        val end = min(j + stepSize, identer.size)
        val digDirResponse = getPeopleDataFromKRR(
            identer.slice(j until end), accessToken = accessToken, navCallId = navCallId
        ) ?: continue
        val filteredPeopleInfo = filerAndMapKRRResponse(digDirResponse)
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
            val requestData: PersonerEndpointRequest
            try {
                requestData = call.receive()
            } catch (e: CannotTransformContentToTypeException) {
                return@post call.respond(HttpStatusCode.BadRequest, e.message.toString())
            }
            val queryParameters = call.request.queryParameters
            val responseFormat = if (queryParameters["type"] == "csv") ResponseFormat.CSV else ResponseFormat.JSON
            val includePdl = queryParameters["pdl"].toBoolean()
            when (val responseStatus =
                getPersonerTotalBulk(requestData, responseFormat, includePdl, call.request.headers)) {
                is PersonerStatus.Error -> call.respond(responseStatus.status, responseStatus.message ?: "")
                is PersonerStatus.SuccessCSV -> call.respondText(responseStatus.csv)
                is PersonerStatus.SuccessJson -> call.respond(responseStatus.response)
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
