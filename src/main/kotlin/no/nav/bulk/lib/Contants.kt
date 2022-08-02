package no.nav.bulk.lib

import io.github.cdimascio.dotenv.dotenv
import no.nav.bulk.models.AzureAdOpenIdConfiguration
import no.nav.bulk.models.buildAzureADConfig

val dotenv = dotenv {
    ignoreIfMissing = true
}

object RunEnv {
    val ENV = dotenv["ENVIRONMENT"] ?: "production"
}

fun RunEnv.isDevelopment() = ENV == "development"
fun RunEnv.isProduction() = ENV == "production"

object AuthConfig {
    const val SCOPE = "api://dev-gcp.team-rocket.digdir-krr-proxy/.default"
    const val TEAM_BULK_GROUP_ID_PROD = "e08a856f-6e64-48b0-978b-5b201760fa13"
    const val TEAM_BULK_GROUP_ID_DEV = "0242dce3-f722-4c6b-ac97-2dd7cc798c4e"
    val CLIENT_ID: String = dotenv["AZURE_APP_CLIENT_ID"]
    val CLIENT_JWK: String = dotenv["AZURE_APP_JWK"]
    private val AZURE_APP_WELL_KNOWN_URL: String = dotenv["AZURE_APP_WELL_KNOWN_URL"]
    val azureADConfig: AzureAdOpenIdConfiguration = buildAzureADConfig(AZURE_APP_WELL_KNOWN_URL)
}

object Endpoints {
    const val DIGDIR_KRR_API_URL = "https://digdir-krr-proxy.dev.intern.nav.no/rest/v1/personer"
    val TOKEN_ENDPOINT: String = dotenv["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"]
}