package no.nav.bulk.plugins

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.bulk.lib.*
import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataRequest

suspend fun personerEndpointResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>, env: String) {

    // get authorization header
    val context = pipelineContext.context
    val call = pipelineContext.call

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
    lateinit var requestData: PeopleDataRequest
    try {
        requestData = call.receive()
    } catch (e: Exception) {
        logger.error(e.message, e)
        when (e) {
            is RequestAlreadyConsumedException,
            is CannotTransformContentToTypeException -> {
                call.respond(HttpStatusCode.BadRequest)
            }
            else -> call.respond(HttpStatusCode.BadRequest) // Muligens endre?
        }
        return@personerEndpointResponse
    }

    val digDirResponse = getContactInfo(
        requestData.personidenter,
        accessToken = tokenEndpointResponse.access_token
    )
    // Add filter here
    if (digDirResponse != null) {
        val filteredPeopleInfo = filterAndMapDigDirResponse(digDirResponse)
        call.respond(filteredPeopleInfo)
    } else
        call.respond(HttpStatusCode.InternalServerError)
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