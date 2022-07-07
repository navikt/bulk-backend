package no.nav.bulk.lib

import io.github.cdimascio.dotenv.dotenv

val dotenv = dotenv {
    ignoreIfMissing = true
}

object AuthConfig {
    const val SCOPE = "api://dev-gcp.team-rocket.digdir-krr-proxy/.default"
    val CLIENT_ID = dotenv["AZURE_APP_CLIENT_ID"] ?: System.getenv("AZURE_APP_CLIENT_ID")
    val CLIENT_SECRET = dotenv["AZURE_APP_CLIENT_SECRET"] ?: System.getenv("AZURE_APP_CLIENT_SECRET")
}

object Endpoints {
    const val DIGDIR_KRR_API_URL = "https://digdir-krr-proxy.dev.intern.nav.no/rest/v1/personer"
    val TOKEN_ENDPOINT = dotenv["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"] ?: System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")
}