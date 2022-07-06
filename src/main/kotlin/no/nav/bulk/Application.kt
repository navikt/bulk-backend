package no.nav.bulk

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import no.nav.bulk.lib.getAccessToken
import no.nav.bulk.lib.getContactInfo
import no.nav.bulk.models.TokenEndpointResponse
import no.nav.bulk.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as CNClient
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as CNServer

lateinit var client: HttpClient

val personer = listOf(
    "07506535861",
    "07428827184",
    "05417034147",
    "29438107647",
    "14466746291",
    "24457907822",
    "01478520936",
    "12476939249",
    "19087222260"
)

fun initializeHttpClient() = runBlocking {
    val newClient = HttpClient(CIO) {
        install(CNClient) {
            json()
        }
    }
    client = newClient
}

fun main() {
    initializeHttpClient()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureHTTP()
        install(CNServer) {
            json()
        }
    }.start(wait = true)
}
