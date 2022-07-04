package no.nav.bulk.lib

object AuthConfig {
    const val SCOPE = "api://dev-gcp.team-rocket.digdir-krr-proxy/.default"
    val CLIENT_ID = System.getenv("AZURE_APP_CLIENT_ID")
    val CLIENT_SECRET = System.getenv("AZURE_APP_CLIENT_SECRET")
    val TOKEN_ENDPOINT = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")
}

object Urls {
    val DIGDIR_KRR_API_URL = "https://digdir-krr-proxy.dev.intern.nav.no/rest/v1/personer"
}