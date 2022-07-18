package no.nav.bulk.lib

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import no.nav.bulk.client
import no.nav.bulk.models.DigDirRequest
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.TokenEndpointResponse
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import java.util.*

suspend fun getAccessToken(clientArg: HttpClient? = null, assertion: String): TokenEndpointResponse? {
    val localClient = clientArg ?: client

    val response = try {
        localClient.post(Endpoints.TOKEN_ENDPOINT) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("scope", AuthConfig.SCOPE)
                        append("client_id", AuthConfig.CLIENT_ID)
                        append("client_secret", AuthConfig.CLIENT_SECRET)
                        append("requested_token", "on_behalf_of")
                        append("assertion", assertion)
                        append("subject_token", "")
                        append("audience", "")
                    }
                )
            )
        }
    } catch (e: ClientRequestException) {
        return null
    }
    return response.body()
}

suspend fun getAccessTokenOBO(token: String): String {
    val tokenClient: AzureAdOnBehalfOfTokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildOnBehalfOfTokenClient()

    return tokenClient.exchangeOnBehalfOfToken(AuthConfig.SCOPE, token)
}

suspend fun getContactInfo(personnr: List<String>,
                           clientArg: HttpClient? = null,
                           accessToken: String): DigDirResponse? {
    val localClient = clientArg ?: client
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
