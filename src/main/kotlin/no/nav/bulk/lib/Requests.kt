package no.nav.bulk.lib

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import no.nav.bulk.client
import no.nav.bulk.models.DigDirRequest
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.TokenEndpointResponse
import java.util.UUID

suspend fun getAccessToken(clientArg: HttpClient? = null): TokenEndpointResponse {
    val localClient = clientArg ?: client
    val response = localClient.post(Endpoints.TOKEN_ENDPOINT) {
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

suspend fun getContactInfo(personnr: List<String>, clientArg: HttpClient? = null): DigDirResponse {
    val localClient = clientArg ?: client
    val accessToken = getAccessToken(localClient)
    val res = client.post(Endpoints.DIGDIR_KRR_API_URL) {
        headers {
            append(HttpHeaders.Authorization, "Bearer ${accessToken.access_token}")
            append("Nav-Call-Id", UUID.randomUUID().toString())
        }
        contentType(ContentType.Application.Json)
        setBody(DigDirRequest(personnr))
    }
    return res.body()
}
