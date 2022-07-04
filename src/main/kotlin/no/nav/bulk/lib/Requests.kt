package no.nav.bulk.lib

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import no.nav.bulk.client
import no.nav.bulk.models.DigDirRequest
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.TokenEndpointResponse
import java.util.UUID

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

suspend fun getContactInfo(personnr: List<String>): DigDirResponse {
    val accessToken = getAccessToken()
    println(accessToken)
    println()
    val res = client.post(Urls.DIGDIR_KRR_API_URL) {
        headers {
            append(HttpHeaders.Authorization, "Bearer ${accessToken.access_token}")
            append("Nav-Call-Id", UUID.randomUUID().toString())
        }
        contentType(ContentType.Application.Json)
        setBody(DigDirRequest(personnr))
    }
    return res.body()
}
