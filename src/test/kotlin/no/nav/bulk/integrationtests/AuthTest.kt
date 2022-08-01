package no.nav.bulk.integrationtests

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import no.nav.bulk.initializeHttpClient
import no.nav.bulk.lib.AuthConfig.CLIENT_ID
import no.nav.bulk.plugins.configureAuth
import no.nav.bulk.plugins.configureHTTP
import no.nav.bulk.plugins.configureRouting
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.event.Level
import kotlin.test.assertEquals

class AuthTest() {
    companion object {
        private val server = MockOAuth2Server()
        const val issuerid = "azure"

        @JvmStatic
        @BeforeAll
        fun setup() {
            initializeHttpClient()
            server.start()
        }
        @JvmStatic
        @AfterAll
        fun tearDown() {
            server.shutdown()
        }
    }

    private fun ApplicationTestBuilder.configTestApp() {
        application {
            initializeHttpClient()
            configureRouting()
            configureHTTP()
            configureAuth(issuerid)

        }
        install(ContentNegotiation) { json() }
        install(CallLogging) {
            level = Level.INFO
        }
    }

    @Test
    fun `http POST to personer endpoint without token should return 401`() = testApplication {
            configTestApp()
            val res = client.post("/personer")
            println(res)
            assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `please work`() = testApplication {
        configTestApp()
        val res = client.post("/personer") {
            header("Authorization", "Bearer ${server.tokenFromProvider1()}")
        }
        println(res.bodyAsText())
        assertEquals(HttpStatusCode.OK, res.status)
    }


    private fun MockOAuth2Server.tokenFromProvider1() =
        issueToken(
            "azure",
            "taper",
            audience = CLIENT_ID,
            claims = mapOf("groups" to listOf("group1", "group2"))
        ).serialize()


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
