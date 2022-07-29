package no.nav.bulk.integrationtests

import org.junit.jupiter.api.Test
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback

class AuthTest() {
    private val server: MockOAuth2Server = MockOAuth2Server()
    private val issuerId: String = "966ac572-f5b7-4bbe-aa88-c76419c0f851"
    private val config: OAuth2Config = OAuth2Config();

    fun `Do some random stuff with auth`() {
        assert(true)
    }
    @Test
    fun loginWithIdTokenForSubjectFoo() {
        server.enqueueCallback(
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = "foo"
            )
        )
        assert(true)
        // Invoke your app here and assert user foo is logged in
    }
}