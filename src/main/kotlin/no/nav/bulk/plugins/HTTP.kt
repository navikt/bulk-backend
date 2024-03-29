package no.nav.bulk.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.partialcontent.*
import java.net.URL
import java.util.concurrent.TimeUnit
import no.nav.bulk.lib.AuthConfig
import no.nav.bulk.lib.RunEnv
import no.nav.bulk.logger
import no.nav.bulk.models.AzureAdOpenIdConfiguration

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeadersPrefixed("Nav-")
        allowHost("localhost:3000")
        allowHost("bulk.dev.intern.nav.no", schemes = listOf("https"))
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(PartialContent)
    install(AutoHeadResponse)
}

fun Application.configureAuth(azureAdConfig: AzureAdOpenIdConfiguration) {
    install(Authentication) {
        jwt {
            // provides a JWTVerifier that is used to verify a token format and signature
            val jwkProvider = buildJwkProvider(azureAdConfig.jwksUri)

            // register the provider
            verifier(jwkProvider, azureAdConfig.issuer)

            validate { credentials: JWTCredential ->
                logger.info("Try to verify token")
                // TODO: do we need to do a request to azure ad to verify that the user is a member
                // of the group?
                try {
                    // token has a subject claim
                    requireNotNull(credentials.payload.subject) {
                        logger.error(
                                "Auth: Missing subject in token: '${credentials.payload.subject}'"
                        )
                    }

                    // token has a valid issuer claim
                    requireNotNull(credentials.payload.issuer) {
                        logger.error("Auth: missing issuer in token")
                    }

                    require(credentials.payload.issuer.equals(azureAdConfig.issuer)) {
                        logger.error(
                                "Auth: Valid issuer not found in token: '${credentials.payload.issuer}'"
                        )
                    }

                    // Token does not have an audience claim
                    requireNotNull(credentials.payload.audience) {
                        logger.error("Auth: Missing audience in token")
                    }
                    require(credentials.payload.audience.contains(AuthConfig.CLIENT_ID)) {
                        logger.error(
                                "Auth: Valid audience not found in claims: '${credentials.payload.audience}' != '${AuthConfig.CLIENT_ID}'"
                        )
                    }

                    val authorizedGroup =
                            if (RunEnv.isProduction()) AuthConfig.TEAM_BULK_GROUP_ID_PROD
                            else AuthConfig.TEAM_BULK_GROUP_ID_DEV

                    require(
                            credentials
                                    .payload
                                    .getClaim("groups")
                                    .asList(String::class.java)
                                    .contains(authorizedGroup)
                    ) {
                        logger.error(
                                "Auth: Valid group not found in claims: ${credentials.payload.getClaim("groups")} != [${AuthConfig.TEAM_BULK_GROUP_ID_PROD}, ${AuthConfig.TEAM_BULK_GROUP_ID_DEV}]"
                        )
                    }

                    logger.info("${credentials.payload.getClaim("name")} is authenticated")
                    return@validate JWTPrincipal(credentials.payload)
                } catch (e: Throwable) {
                    logger.error("Auth: Error validating token ${e.message}")
                    return@validate null
                }
            }
        }
    }
}

fun buildJwkProvider(jwkProviderUrl: String): JwkProvider {
    // https://github.com/nais/examples/blob/main/sec-blueprints/service-to-service/api-onbehalfof-ktor/src/main/kotlin/no/nav/security/examples/ProtectedOnBehalfOfApp.kt
    return JwkProviderBuilder(URL(jwkProviderUrl))
            .cached(10, 24, TimeUnit.HOURS) // cache up to 10 JWKs for 24 hours
            .rateLimited(
                    10,
                    1,
                    TimeUnit.MINUTES
            ) // if not cached, only allow max 10 different keys per minute to be fetched from
            // external provider
            .build()
}
