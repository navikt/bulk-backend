package no.nav.bulk.lib

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import no.nav.bulk.client
import no.nav.bulk.models.TokenEndpointResponse

suspend fun getAccessToken(): TokenEndpointResponse {
    val response = client.post(AuthConfig.TOKEN_ENDPOINT) {
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("grant_type", "client_credentials")
                    append("scope", AuthConfig.SCOPE)
                    append("client_id", AuthConfig.CLIENT_ID)
                    append("client_secret", AuthConfig.CLIENT_SECRET)
                }
            )
        )
    }
    return response.body()
}
