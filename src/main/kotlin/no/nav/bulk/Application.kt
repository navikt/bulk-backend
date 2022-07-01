package no.nav.bulk

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.bulk.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as CNClient
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as CNServer
import kotlinx.coroutines.runBlocking

lateinit var client: HttpClient

fun initializeHttpClient() {
    runBlocking {
        val newClient = HttpClient(CIO) {
            install(CNClient) {
                json()
            }
        }
        client = newClient
    }
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
