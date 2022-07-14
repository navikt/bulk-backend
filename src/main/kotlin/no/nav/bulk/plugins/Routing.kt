package no.nav.bulk.plugins

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.bulk.lib.filterAndMapDigDirResponse
import no.nav.bulk.lib.getAccessToken
import no.nav.bulk.lib.getContactInfo
import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataRequest

fun Application.configureRouting() {

    routing {
        get("/isalive") {
            val wonderwallToken = this.context.getWonderwallAccessToken()
            println(wonderwallToken)
            call.respond("Alive" + "\n${wonderwallToken}")
        }

        get("/isready") {
            call.respond("Ready")
        }

        authenticate {
            post("/personer") {
                // get authorization header
                val wonderWallccessToken = context.getWonderwallAccessToken()

                // parse token
                val jwt = JWT().decodeJwt(wonderWallccessToken)
                val assertion = jwt.token

                // request now token scoped to the downstream api resource
                val tokenEndpointResponse = getAccessToken(null, assertion)

                // return if invalid
                if (tokenEndpointResponse == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
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
                    return@post
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