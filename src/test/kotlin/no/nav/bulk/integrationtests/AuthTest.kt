package no.nav.bulk.integrationtests

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import no.nav.bulk.initializeHttpClient
import no.nav.bulk.lib.AuthConfig
import no.nav.bulk.lib.AuthConfig.CLIENT_ID
import no.nav.bulk.models.buildAzureADConfig
import no.nav.bulk.plugins.configureAuth
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.event.Level
import java.net.InetAddress
import java.util.UUID

class AuthTest {

    /**
      *  AuthTest currently generates JWT tokens using the mock0auth2-server from NAV IKT.
      *  The extension methods creates both valid and invalid tokens, and are used to test different responses from the
      *  auth endpoint, because it only tests the token verification and validation.
      */

    companion object {
        private val server = MockOAuth2Server(config=OAuth2Config.fromJson(MOCK_AUTH_JSON_CONFIG))
        private const val issuerId = "azure"
        private lateinit var wellKnownUrl: String

        @JvmStatic @BeforeAll
        fun setUp() {
            server.start(InetAddress.getByName("localhost"), 8080)
            wellKnownUrl = server.wellKnownUrl(issuerId).toString()
            server.wellKnownUrl(issuerId)
        }
        @JvmStatic @AfterAll
        fun tearDown() {
            server.shutdown()
        }
    }

    private fun ApplicationTestBuilder.configTestApp(jwkProviderUrl: String) {
        initializeHttpClient()
        application {
            configureRouting()
            configureHTTP()
            val azureAdTestConfig = buildAzureADConfig(jwkProviderUrl)
            configureAuth(azureAdTestConfig)
        }
        install(ContentNegotiation) { json() }
        install(CallLogging) {
            level = Level.INFO
        }
    }

    @Test
    fun `http POST to personer endpoint without token should return 401`() = testApplication {
            configTestApp(wellKnownUrl)
            val res = client.post("/personer")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `http GET to auth test endpoint with a valid token should return 200`() = testApplication {
        configTestApp(wellKnownUrl)
        val res = client.get("/auth") {
            header("Authorization", "Bearer ${server.validTokenFromAzureAd()}")
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `http GET to auth test endpoint with an invalid groups claim should return 401`() = testApplication {
        configTestApp(wellKnownUrl)
        val res = client.get("/auth") {
            header("Authorization", "Bearer ${server.invalidGroupsClaimTokenFromAzureAd()}")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `http GET to auth test endpoint with an invalid expiration claim should return 401`() = testApplication {
        configTestApp(wellKnownUrl)
        val res = client.get("/auth") {
            header("Authorization", "Bearer ${server.invalidExpirationTokenFromAzureAd()}")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    private fun MockOAuth2Server.validTokenFromAzureAd() =
        issueToken(
            issuerId = issuerId,
            subject = UUID.randomUUID().toString(),
            audience = CLIENT_ID,
            claims = mapOf(
                "groups" to listOf(AuthConfig.TEAM_BULK_GROUP_ID_DEV),
                "name" to "Taper1"
            )
        ).serialize()

    private fun MockOAuth2Server.invalidGroupsClaimTokenFromAzureAd() =
        issueToken(
            issuerId = issuerId,
            subject = UUID.randomUUID().toString(),
            audience = CLIENT_ID,
            claims = mapOf(
                "groups" to listOf(UUID.randomUUID()),
                "name" to "Taper2"
            )
        ).serialize()

    private fun MockOAuth2Server.invalidExpirationTokenFromAzureAd() =
        issueToken(
            issuerId = issuerId,
            subject = UUID.randomUUID().toString(),
            audience = CLIENT_ID,
            claims = mapOf(
                "groups" to listOf(AuthConfig.TEAM_BULK_GROUP_ID_DEV),
                "name" to "Taper3",
            ),
            expiry = -1
        ).serialize()
}
