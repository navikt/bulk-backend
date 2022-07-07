package no.nav.bulk.lib

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.util.UUID
import no.nav.bulk.client
import no.nav.bulk.models.DigDirRequest
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.TokenEndpointResponse

suspend fun getAccessToken(clientArg: HttpClient? = null): TokenEndpointResponse? {
    val localClient = clientArg ?: client

    val response = try {
        localClient.post(Endpoints.TOKEN_ENDPOINT) {
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
    } catch (e: ClientRequestException) {
        return null
    }
    return response.body()
}

suspend fun getContactInfo(personnr: List<String>, clientArg: HttpClient? = null): DigDirResponse? {
    val localClient = clientArg ?: client
    val accessToken = getAccessToken(localClient)?.access_token
    val res = try {
        localClient.post(Endpoints.DIGDIR_KRR_API_URL) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
                append("Nav-Call-Id", UUID.randomUUID().toString())
            }
            contentType(ContentType.Application.Json)
            setBody(DigDirRequest(personnr))
        }
    } catch (e: ClientRequestException) {
        return null
    }
    return res.body()
}
