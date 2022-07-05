package no.nav.bulk.lib

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.*
import no.nav.bulk.models.PersonInfoRequest
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestsTest {
    private var applicationBuilder: ApplicationTestBuilder? = null
    @BeforeAll
    fun setup() {
        testApplication {
            application {
                configureHTTP()
                configureRouting()
                install(ContentNegotiation) {
                    json()
                }
            }
            applicationBuilder = this
        }
    }
    @Test
    fun testIsAlive() {
        val task: Job = CoroutineScope(Dispatchers.IO).launch {
            val response = applicationBuilder!!.client.get("/isalive")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("\"Alive\"", response.body())
            assertTrue("contains quotes") {
                response.body<String>().contains("\"")
            }
        }
        runBlocking {
            task.join()
        }
    }
    @Test
    fun testIsReady() {
        val task: Job = CoroutineScope(Dispatchers.IO).launch {
            val response = applicationBuilder!!.client.get("/isready")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("\"Ready\"", response.body())
            assertTrue("contains quotes") {
                response.body<String>().contains("\"")
            }
        }
        runBlocking {
            task.join()
        }
    }
    @OptIn(InternalAPI::class)
    @Test
    fun testIsPersoner() {
        val testPersonidenter = PersonInfoRequest(listOf("12345678901", "12345678902", "12345678903"))
        val task: Job = CoroutineScope(Dispatchers.IO).launch {
            val response = applicationBuilder!!.client.get("/personer") {
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json)
                }
                body = testPersonidenter.toString()
            }

            assertEquals("", response.body())
        }
        runBlocking {
            task.join()
        }
    }
    @Test
    fun testGetAccessToken() = testApplication {
        val tokenEndpointResponse = getAccessToken()
        assertEquals(3599, tokenEndpointResponse.expires_in)
        assertEquals(true, tokenEndpointResponse.access_token.isNotEmpty())
        assertEquals("Bearer", tokenEndpointResponse.token_type)
    }
}