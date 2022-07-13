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
import no.nav.bulk.lib.AuthConfig
import no.nav.bulk.plugins.configureAuth
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
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
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
    client = newClient
}

fun main() {
    initializeHttpClient()
    val env = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
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
