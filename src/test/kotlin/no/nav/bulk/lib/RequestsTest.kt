package no.nav.bulk.lib

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import no.nav.bulk.initializeHttpClient
import kotlin.test.*
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting

class RequestsTest {
    @Test
    fun testIsAlive() = testApplication {
        application {
            configureHTTP()
            configureRouting()
            install(ContentNegotiation) { json() }
        }
        val response = this.client.get("/isalive")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("\"Alive\"", response.body())
        assertTrue("contains quotes") { response.body<String>().contains("\"") }
    }


    @Test
    fun testIsReady() = testApplication {
        application {
            configureHTTP()
            configureRouting()
            install(ContentNegotiation) { json() }
        }
        val response = this.client.get("/isready")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("\"Ready\"", response.body())
        assertTrue("contains quotes") { response.body<String>().contains("\"") }
    }

    @Test
    fun testPersoner() = testApplication {
        application {
            configureHTTP()
            configureRouting()
            install(ContentNegotiation) { json() }
        }
        val testPersonidenter =
            listOf(
                "07506535861",
                "07428827184",
            )
        var response: DigDirResponse?
        response = getContactInfo(testPersonidenter, client)
        println(response)
        assertEquals(true, response != null)
        assertEquals(true, false)
    }


    @Test
    fun testGetAccessToken() = testApplication {
        initializeHttpClient()
        val tokenEndpointResponse = getAccessToken()
        assertEquals(3599, tokenEndpointResponse.expires_in)
        assertEquals(true, tokenEndpointResponse.access_token.isNotEmpty())
        assertEquals("Bearer", tokenEndpointResponse.token_type)
    }

}
