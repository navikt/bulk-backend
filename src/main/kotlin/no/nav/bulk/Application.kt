package no.nav.bulk

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.bulk.plugins.configureAuth
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as CNClient
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as CNServer

lateinit var client: HttpClient

fun initializeHttpClient() = runBlocking {
    val newClient = HttpClient(CIO) {
        install(CNClient) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
    client = newClient
}

val logger: Logger = LoggerFactory.getLogger("no.nav.bulk")

fun main() {
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    initializeHttpClient()
    val env = applicationEngineEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load())
        module {
            configureRouting()
            configureHTTP()
            configureAuth()
            install(CNServer) { json() }
            install(CallLogging) {
                level = Level.INFO
            }
        }
        connector {
            port = 8080
            host = "0.0.0.0"
        }
    }
    embeddedServer(Netty, environment = env).start(wait = true)
}
