package no.nav.bulk.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import no.nav.bulk.lib.AuthConfig
import java.net.URL
import java.util.concurrent.TimeUnit

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeadersPrefixed("Nav-")
        allowHost("localhost:3000")
        allowHost("bulk.dev.intern.nav.no")
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
}

fun Application.configureAuth() {
    install(Authentication) {
        jwt {
            // provides a JWTVerifier that is used to verify a token format and signature
            val jwkProvider = buildJwkProvider()

            // register the provider
            verifier(jwkProvider, AuthConfig.azureadConfig.issuer)

            validate { credentials: JWTCredential ->
                try {
                    requireNotNull(credentials.payload.audience) {
                        "Auth: Missing audience in token"
                    }
                    require(credentials.payload.audience.contains(AuthConfig.CLIENT_ID)) {
                        "Auth: Valid audience not found in claims"
                    }
                    JWTPrincipal(credentials.payload)
                } catch (e: Throwable) {
                    null
                }
            }
        }
    }
}


fun buildJwkProvider(): JwkProvider {
    // https://github.com/nais/examples/blob/main/sec-blueprints/service-to-service/api-onbehalfof-ktor/src/main/kotlin/no/nav/security/examples/ProtectedOnBehalfOfApp.kt
    return JwkProviderBuilder(URL(AuthConfig.AZURE_APP_WELL_KNOWN_URL))
        .cached(10, 24, TimeUnit.HOURS) // cache up to 10 JWKs for 24 hours
        .rateLimited(
            10,
            1,
            TimeUnit.MINUTES
        ) // if not cached, only allow max 10 different keys per minute to be fetched from external provider
        .build()
}