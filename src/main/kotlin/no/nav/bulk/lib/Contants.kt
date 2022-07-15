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

object AuthConfig {
    const val SCOPE = "api://dev-gcp.team-rocket.digdir-krr-proxy/.default"
    val CLIENT_ID = (dotenv["AZURE_APP_CLIENT_ID"] ?: System.getenv("AZURE_APP_CLIENT_ID"))!!
    val CLIENT_SECRET = (dotenv["AZURE_APP_CLIENT_SECRET"] ?: System.getenv("AZURE_APP_CLIENT_SECRET"))!!
    val AZURE_APP_WELL_KNOWN_URL = (dotenv["AZURE_APP_WELL_KNOWN_URL"] ?: System.getenv("AZURE_APP_WELL_KNOWN_URL"))!!
    val azureadConfig: AzureAdOpenIdConfiguration = runBlocking {
        client.get(AZURE_APP_WELL_KNOWN_URL).body()
    }
}

object Endpoints {
    const val DIGDIR_KRR_API_URL = "https://digdir-krr-proxy.dev.intern.nav.no/rest/v1/personer"
    val TOKEN_ENDPOINT = (dotenv["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"] ?: System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"))!!
}