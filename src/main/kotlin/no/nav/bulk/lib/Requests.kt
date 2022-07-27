package no.nav.bulk.lib

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import no.nav.bulk.client
import no.nav.bulk.models.DigDirRequest
import no.nav.bulk.models.DigDirResponse
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import java.util.*

fun getAccessToken(accessToken: String): String? {
    val builder: AzureAdTokenClientBuilder = AzureAdTokenClientBuilder.builder()
    val tokenClient: AzureAdOnBehalfOfTokenClient = builder
        .withClientId(AuthConfig.CLIENT_ID)
        .withPrivateJwk(AuthConfig.CLIENT_JWK)
        .withTokenEndpointUrl(Endpoints.TOKEN_ENDPOINT)
        .buildOnBehalfOfTokenClient()

    return tokenClient.exchangeOnBehalfOfToken(AuthConfig.SCOPE, accessToken)
}

suspend fun getContactInfo(
    personnr: List<String>,
    clientArg: HttpClient? = null,
    accessToken: String
): DigDirResponse? {
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
    } catch (e: Exception) {
        return when (e) {
            is ClientRequestException,
            is ServerResponseException,
            is SerializationException -> null
            else -> throw e
        }
    }

    return if (res.status.isSuccess())
        res.body()
    else null
}
