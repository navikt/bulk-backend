package no.nav.bulk.lib

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import org.junit.After
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestsTest {
    private lateinit var applicationBuilder: ApplicationTestBuilder
    @OptIn(DelicateCoroutinesApi::class)
    private val mainThreadSurrogate = newSingleThreadContext("Testing thread")

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeAll
    fun setUpThreadDispatcher() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun testSimple(): Unit = runBlocking {
        launch(Dispatchers.Main) {
            assertEquals(2 + 3, 5)
            println("turtleneck tuesday")

            val response = applicationBuilder.client.get("/isalive")
            println(HttpStatusCode.OK.toString() + response.status.toString())
            assertEquals("\"Alive\"", response.body())
            assertTrue("contains quotes") {
                response.body<String>().contains("\"")
            }
            assertEquals(2+2, 10000)
        }
    }

    @Test
    fun testIsAlive() {
        val task: Job = CoroutineScope(Dispatchers.IO).launch {
            val response = applicationBuilder.client.get("/isalive")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("\"Alive\"", response.body())
            assertTrue("contains quotes") {
                response.body<String>().contains("\"")
            }
            assertEquals(2+2, 10000)
        }
        runBlocking {
            task.join()
        }
    }

    @Test
    fun testIsReady() {
        val task: Job = CoroutineScope(Dispatchers.IO).launch {
            val response = applicationBuilder.client.get("/isready")
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

    @Test
    fun testPersoner() {
        val testPersonidenter = listOf("07506535861", "07428827184",)
        var response: DigDirResponse?
        val job = CoroutineScope(Dispatchers.IO).launch {
            response = getContactInfo(testPersonidenter, applicationBuilder.client)
            println(response)
            assertEquals(true, response != null)
            assertEquals(true, false)

        }
        runBlocking {
            job.join()
        }
    }

    @Test
    fun testGetAccessToken() {
        val task: Job = CoroutineScope(Dispatchers.IO).launch {
            val tokenEndpointResponse = getAccessToken(applicationBuilder.client)
            assertEquals(3599, tokenEndpointResponse.expires_in)
            assertEquals(true, tokenEndpointResponse.access_token.isNotEmpty())
            assertEquals("Bearer", tokenEndpointResponse.token_type)
        }
        runBlocking {
            task.join()
        }
    }
}