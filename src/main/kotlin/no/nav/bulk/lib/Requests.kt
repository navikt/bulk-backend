package no.nav.bulk.lib

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import no.nav.bulk.client
import no.nav.bulk.models.DigDirRequest
import no.nav.bulk.models.DigDirResponse
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

//suspend fun getPnrsNames(identer: List<String> = listOf("11817798936", "13840149832", "15899796796", "24867598509"), accessToken: String) {
//    logger.info("")
//    val query = "query(\$identer: [ID!]!) { hentPersonBolk(identer: \$identer) { ident, person { navn { fornavn mellomnavn etternavn forkortetNavn } }, code } }"
//    val res = try {
//        client.post("https://pdl-api.dev-fss-pub.nais.io/graphql") {
//            headers {
//                append(HttpHeaders.Authorization, "Bearer $accessToken")
//                append("Tema", "GEN")
//            }
//            contentType(ContentType.Application.Json)
//            setBody(Body(query, Variables(identer)))
//        }
//    } catch (e: Exception) {
//        println(e.message)
//        return
//    }
//    println(res.bodyAsText())
//    val tmp  = "22"
//}
//
//@Serializable
//data class Body(val query: String, val variables: Variables)
//
//@Serializable
//data class Variables(val identer: List<String>)
