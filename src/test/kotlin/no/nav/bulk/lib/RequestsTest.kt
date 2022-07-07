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
    fun testInvalidPersons() = testApplication {
        application {
            configureHTTP()
            configureRouting()
            install(ContentNegotiation) { json() }
        }
        initializeHttpClient()
        val testPersonidenter = listOf(
            "07506535861",
            "07428827184",
            "1234",
            "11111100000"
        )
        val response = getContactInfo(testPersonidenter)

        assertEquals(2, response.feil.size)
        assertEquals("person_ikke_funnet", response.feil["1234"])
        assertEquals("person_ikke_funnet", response.feil["11111100000"])
    }

    @Test
    fun testPersons() = testApplication {
        application {
            configureHTTP()
            configureRouting()
            install(ContentNegotiation) { json() }
        }
        initializeHttpClient()
        val testPersonidenter = listOf(
            "07506535861",
            "07428827184",
        )
        val response = getContactInfo(testPersonidenter)

        assertEquals(true, response.personer.isNotEmpty())
        assertEquals("nn", response.personer["07506535861"]?.spraak)
        assertEquals(true, response.personer["07506535861"]?.kanVarsles)
        assertEquals(2, response.personer.size)
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
