package no.nav.bulk.lib

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.bulk.client
import no.nav.bulk.models.AzureAdOpenIdConfiguration

val dotenv = dotenv {
    ignoreIfMissing = true
}

object RunEnv {
    val ENV = dotenv["ENVIRONMENT"] ?: "production"
}

object AuthConfig {
    const val SCOPE = "api://dev-gcp.team-rocket.digdir-krr-proxy/.default"
    val CLIENT_ID: String = dotenv["AZURE_APP_CLIENT_ID"]
    val CLIENT_JWK: String = dotenv["AZURE_APP_JWK"]
    private val AZURE_APP_WELL_KNOWN_URL: String = dotenv["AZURE_APP_WELL_KNOWN_URL"]
    val FRONTEND_CLIENT_ID: String = dotenv["FRONTEND_CLIENT_ID"]
    val azureADConfig: AzureAdOpenIdConfiguration = runBlocking {
        client.get(AZURE_APP_WELL_KNOWN_URL).body()
    }
}

object Endpoints {
    const val DIGDIR_KRR_API_URL = "https://digdir-krr-proxy.dev.intern.nav.no/rest/v1/personer"
    val TOKEN_ENDPOINT: String = dotenv["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"]
}