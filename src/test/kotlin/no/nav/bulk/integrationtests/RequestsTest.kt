package no.nav.bulk.integrationtests

import com.auth0.jwt.JWT
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.ktor.server.util.*
import io.ktor.util.*
import no.nav.bulk.initializeHttpClient
import no.nav.bulk.lib.AuthConfig
import no.nav.bulk.lib.getAccessToken
import no.nav.bulk.lib.getContactInfo
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestsTest {
    /*
        Test cases:
            1. Test if the applications responds on the /isalive route, with the correct status and response call.
            2. Test if the applications responds on the /isready route, with the correct status and response call.
            3. Test that invalid personidents responds with the correct error message, specified in the
               "feil" Map.
            4. Checks that the call to getContactInfo returns the personidents with additional contact info,
               and correct language and "kanVarsles"-status. Initializes the HTTP-client as well.
            5. Checks that the TokenEndPointResponse gets generated and that the variables (token_type and
               expires_in) are correct.
     */

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
        try {
            initializeHttpClient()
            val testPersonidenter = listOf(
                "07506535861",
                "07428827184",
                "1234",
                "11111100000"
            )
            val accessToken = getAccessToken() ?: ""
            val response = getContactInfo(testPersonidenter, accessToken = accessToken)

            if (response != null) {
                assertEquals(2, response.feil.size)
                assertEquals("person_ikke_funnet", response.feil["1234"]?.value)
                assertEquals("person_ikke_funnet", response.feil["11111100000"]?.value)
            }
        } catch (e: Exception) {
            println(e.stackTrace)
            throw e
        }

    }

    @Test
    fun testPersons() = testApplication {
        application {
            configureHTTP()
            configureRouting()
            install(ContentNegotiation) { json() }
        }

        try {
            initializeHttpClient()
            val testPersonidenter = listOf(
                "07506535861",
                "07428827184",
            )
            val accessToken = getAccessToken() ?: ""
            val response = getContactInfo(testPersonidenter, accessToken = accessToken)

            if (response != null) {
                assertEquals(true, response.personer.isNotEmpty())
                assertEquals("nn", response.personer["07506535861"]?.spraak)
                assertEquals(true, response.personer["07506535861"]?.kanVarsles)
                assertEquals(2, response.personer.size)
            }

        } catch (e: Exception) {
            println(e.stackTrace)
            throw e
        }
    }

    @OptIn(InternalAPI::class)
    @Test
    fun testGetAccessToken() = testApplication {
        try {
            initializeHttpClient()
            val time = LocalDateTime.now()
            val tokenEndpointResponse = getAccessToken()
            assertTrue(tokenEndpointResponse != null, "Token is not null")

            val decodedJwt = JWT.decode(tokenEndpointResponse)

            assertEquals("RS256", decodedJwt.algorithm)
            assertEquals(AuthConfig.azureADConfig.issuer, decodedJwt.issuer)
            assertEquals("\"${AuthConfig.CLIENT_ID}\"", decodedJwt.getClaim("azp").toString())
            assertTrue(time.isBefore(decodedJwt.expiresAt.toLocalDateTime()))
            assertTrue(decodedJwt.issuedAt.before(decodedJwt.expiresAt))
        } catch (e: Exception) {
            println(e.stackTrace)
            throw e
        }
    }
}
