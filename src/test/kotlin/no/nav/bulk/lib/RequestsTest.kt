package no.nav.bulk.lib

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import no.nav.bulk.initializeHttpClient
import no.nav.bulk.main
import no.nav.bulk.personer
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestsTest {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var job: Job

    @BeforeAll
    fun runServer() {
        initializeHttpClient()

        testApplication {

            routing {
                get("/isalive-test") {
                    call.respond("Alive in the test!")
                }
            }
        }
        println("hei")
    }

    @AfterAll
    fun stopServer() {
        println("ferdig")
    }

    /*
    @Test
    fun testIsAlive() = testApplication {
        val response = client.get("/isalive")
        println(response)
        assertEquals(HttpStatusCode.OK, response.status)
        //assertEquals(HttpStatusCode.OK, response.status)
    }

     */

    @Test
    fun testGetAccessToken() = testApplication {
        val tokenEndpointResponse = getAccessToken()

        assertEquals(3599, tokenEndpointResponse.expires_in)
        assertEquals(true, tokenEndpointResponse.access_token.isNotEmpty())
        assertEquals("Bearer", tokenEndpointResponse.token_type)
    }
}

