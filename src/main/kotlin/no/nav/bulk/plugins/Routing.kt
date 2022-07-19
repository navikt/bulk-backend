package no.nav.bulk.plugins

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import mapToCSV
import no.nav.bulk.lib.*
import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataRequest


enum class ResponseFormat {
    JSON,
    CSV
}

suspend fun personerEndpointResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>, env: String) {

    // get authorization header
    val context = pipelineContext.context
    val call = pipelineContext.call
    lateinit var requestData: PeopleDataRequest
    val responseFormat =
        if (call.request.queryParameters["type"] == "csv") ResponseFormat.CSV else ResponseFormat.JSON


    // request now token scoped to the downstream api resource
    val tokenEndpointResponse =
        if (env == "development") {
            getAccessTokenClientCredentials(null)
        } else {
            val wonderWallccessToken = context.getWonderwallAccessToken()

            // parse token
            val jwt = JWT().decodeJwt(wonderWallccessToken)
            val assertion = jwt.token
            getAccessTokenOBO(null, assertion)
        }

    // return if invalid
    if (tokenEndpointResponse == null) {
        call.respond(HttpStatusCode.Unauthorized)
        return@personerEndpointResponse
    }
    try {
        requestData = call.receive()
    } catch (e: ContentTransformationException) {
        logger.error(e.message, e)
        call.respond(HttpStatusCode.BadRequest)
        return@personerEndpointResponse
    }

    val digDirResponse = getContactInfo(
        requestData.personidenter,
        accessToken = tokenEndpointResponse.access_token
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
            val wonderwallToken = this.context.getWonderwallAccessToken()
            logger.info("Authorization: Bearer $wonderwallToken")
            call.respond("Alive" + "\n${wonderwallToken}")
        }

        get("/isready") {
            call.respond("Ready")
        }

        // Authenticate user only in production
        if (RunEnv.ENV == "development") {
            post(personerEndpointString) {
                personerEndpointResponse(this, RunEnv.ENV)
            }
        } else {
            authenticate {
                post(personerEndpointString) {
                    personerEndpointResponse(this, RunEnv.ENV)
                }
            }
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


fun ApplicationCall.getWonderwallAccessToken(): String = request.parseAuthorizationHeader().toString()