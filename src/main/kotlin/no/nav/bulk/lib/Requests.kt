package no.nav.bulk.lib

import AuthConfig
import io.ktor.client.call.*
import io.ktor.client.request.*
import no.nav.bulk.client
import no.nav.bulk.models.TokenEndpointResponse

suspend fun getAccessToken(): TokenEndpointResponse {
    val response = client.post(AuthConfig.TOKEN_ENDPOINT) {
        headers {
            append("scope", AuthConfig.SCOPE)
            append("client_id", AuthConfig.CLIENT_ID)
            append("client_secret", AuthConfig.CLIENT_SECRET)
            append("grant_type", "client_credentials")
        }
    }
    return response.body()
}
