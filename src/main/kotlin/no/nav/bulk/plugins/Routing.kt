package no.nav.bulk.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.RSAKeyProvider
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.bulk.lib.*
import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataRequest

fun Application.configureRouting() {

    routing {
        authenticate {
            get("/isalive") {
                val wonderwallToken = this.context.getWonderwallAccessToken()
                logger.info("Authorization: Bearer $wonderwallToken")
                call.respond("Alive" + "\n${wonderwallToken}")
            }
        }

        get("/isready") {
            call.respond("Ready")
        }

        authenticate {
            post("/personer") {
                // get authorization header
                val wonderWallccessToken = context.getWonderwallAccessToken()
                val tokenWithoutBearer = wonderWallccessToken.split(" ")[1]
                // parse token
                val jwtDecodedTokenPreviousAud: DecodedJWT = JWT.decode(tokenWithoutBearer)

                val test = generateJwt(jwtDecodedTokenPreviousAud)

                val assertion = jwtDecodedTokenPreviousAud.token


                // request now token scoped to the downstream api resource
                val tokenEndpointResponse = getAccessTokenOBO(assertion)

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
                    accessToken = tokenEndpointResponse
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