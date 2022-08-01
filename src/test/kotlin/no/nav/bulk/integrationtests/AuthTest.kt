package no.nav.bulk.integrationtests

import io.ktor.client.request.*
import io.ktor.client.statement.*
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
import org.junit.jupiter.api.Test
import org.slf4j.event.Level
import kotlin.test.assertEquals

class AuthTest() {
//    companion object {
//        private val server = MockOAuth2Server(config=OAuth2Config.fromJson(MOCK_AUTH_JSON_CONFIG))
//        const val issuerId = "azure"
//        lateinit var jwkProviderUrl: String
//
//        @BeforeAll
//        fun setUp() {
//            println("SEEEETUPPPPP")
//            initializeHttpClient()
//            jwkProviderUrl = server.wellKnownUrl(issuerId).toString()
//            server.start(InetAddress.getByName("0.0.0.0"), 8080)
//            server.wellKnownUrl(issuerId)
//        }
//
//        @AfterAll
//        fun tearDown() {
//            server.shutdown()
//        }
//    }

//    private fun ApplicationTestBuilder.configTestApp() {
//        application {
//            initializeHttpClient()
//            configureRouting()
//            configureHTTP()
//            val azureAdTestConfig = buildAzureADConfig(jwkProviderUrl)
//            configureAuth(azureAdTestConfig)
//
//        }
//        install(ContentNegotiation) { json() }
//        install(CallLogging) {
//            level = Level.INFO
//        }
//    }

//    @Test
//    fun `http POST to personer endpoint without token should return 401`() = testApplication {
//            configTestApp()
//            val res = client.post("/personer")
//            println(res)
//            assertEquals(HttpStatusCode.Unauthorized, res.status)
//    }
    private val issuerId = "azure"

    @Test
    fun `http GET to auth test endpoint with a valid token should return 200`() = testApplication {
        val server = MockOAuth2Server(config=OAuth2Config.fromJson(MOCK_AUTH_JSON_CONFIG))
        server.start()
        val wellKnownUrl = server.wellKnownUrl(issuerId).toString()
        initializeHttpClient()
        application {
            initializeHttpClient()
            configureRouting()
            configureHTTP()
            val azureAdTestConfig = buildAzureADConfig(wellKnownUrl)
            configureAuth(azureAdTestConfig)
        }
        install(ContentNegotiation) { json() }
        install(CallLogging) {
            level = Level.INFO
        }
        val res = client.get("/auth") {
            header("Authorization", "Bearer ${server.tokenFromAzureAd()}")
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }


    private fun MockOAuth2Server.tokenFromAzureAd() =
        issueToken(
            issuerId = issuerId,
            subject = "taper",
            audience = CLIENT_ID,
            claims = mapOf(
                "groups" to listOf(AuthConfig.TEAM_BULK_GROUP_ID_DEV)
            )
        ).serialize()


//    @Test
//    fun loginWithIdTokenForSubjectFoo() {
//        server.enqueueCallback(
//            DefaultOAuth2TokenCallback(
//                issuerId = issuerId,
//                subject = "foo"
//            )
//        )
//        // Invoke your app here and assert user foo is logged in
//    }

    /*
    @Test
    fun `login with google or github should return appropriate subject`() {
        mockOAuth2Server.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = "google", subject = "googleSubject"))
        mockOAuth2Server.enqueueCallback(DefaultOAuth2TokenCallback(issuerId = "github", subject = "githubSubject"))

        val port = randomPort()

        withEmbeddedServer(
            { module(authConfig()) },
            port
        ) {
            get<String>("http://localhost:$port/login/google").asClue {
                it shouldBe "welcome googleSubject"
            }
            get<String>("http://localhost:$port/login/github").asClue {
                it shouldBe "welcome githubSubject"
            }
        }
    }

    private inline fun <reified R> get(url: String): R = runBlocking { httpClient.prepareGet(url).body() }

    private fun <R> withEmbeddedServer(
        moduleFunction: Application.() -> Unit,
        port: Int,
        test: ApplicationEngine.() -> R
    ): R {
        val engine = embeddedServer(Netty, port = port) {
            moduleFunction(this)
        }
        engine.start()
        try {
            return engine.test()
        } finally {
            engine.stop(0L, 0L)
        }
    }

     */
}
