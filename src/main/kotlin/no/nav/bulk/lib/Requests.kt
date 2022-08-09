package no.nav.bulk.lib

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import no.nav.bulk.client
import no.nav.bulk.generated.PdlQuery
import no.nav.bulk.gqlClient
import no.nav.bulk.logger
import no.nav.bulk.models.DigDirRequest
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.PDLResponse
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient

fun getAccessTokenOBO(scope: String, accessToken: String): String? {
    val builder: AzureAdTokenClientBuilder = AzureAdTokenClientBuilder.builder()
    val tokenClient: AzureAdOnBehalfOfTokenClient =
        builder.withClientId(AuthConfig.CLIENT_ID)
            .withPrivateJwk(AuthConfig.CLIENT_JWK)
            .withTokenEndpointUrl(Endpoints.TOKEN_ENDPOINT)
            .buildOnBehalfOfTokenClient()

    return tokenClient.exchangeOnBehalfOfToken(scope, accessToken)
}

fun getAccessTokenClientCredentials(scope: String): String? {
    val builder: AzureAdTokenClientBuilder = AzureAdTokenClientBuilder.builder()
    val tokenClient: AzureAdMachineToMachineTokenClient? =
        builder.withClientId(AuthConfig.CLIENT_ID)
            .withPrivateJwk(AuthConfig.CLIENT_JWK)
            .withTokenEndpointUrl(Endpoints.TOKEN_ENDPOINT)
            .buildMachineToMachineTokenClient()

    return tokenClient?.createMachineToMachineToken(scope)
}

suspend fun getContactInfo(
    personnr: List<String>,
    clientArg: HttpClient? = null,
    accessToken: String,
    navCallId: String
): DigDirResponse? {
    val localClient = clientArg ?: client
    val res =
        try {
            localClient.post(Endpoints.DIGDIR_KRR_API_URL) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                    append("Nav-Call-Id", navCallId)
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

// TODO: Change return type from null to actual error codes
suspend fun getPDLInfo(identer: List<String>, accessToken: String): PDLResponse? {
    val pdlQuery = PdlQuery(PdlQuery.Variables(identer))
    val result: GraphQLClientResponse<PdlQuery.Result> = gqlClient.execute(pdlQuery) {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        header("Tema", "GEN")
    }
    val pdlResult = result.data
    return if (pdlResult == null) {
        logger.error("Error in GraphQL query: ${result.errors?.joinToString { it.message }}")
        null
    } else {
        pdlResult.mapPersonBolkResultToPDLResponse()
    }
}

fun PdlQuery.Result.mapPersonBolkResultToPDLResponse(): PDLResponse {
    return hentPersonBolk.associateBy({ it.ident }, { it.person })
}