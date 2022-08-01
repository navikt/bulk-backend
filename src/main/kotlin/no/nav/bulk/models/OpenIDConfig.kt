package no.nav.bulk.models

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.bulk.client

@Serializable
data class AzureAdOpenIdConfiguration(
    @SerialName("jwks_uri")
    val jwksUri: String,
    @SerialName("issuer")
    val issuer: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String
)

fun buildAzureADConfig(jwkProviderUrl: String): AzureAdOpenIdConfiguration {
    return runBlocking {
        client.get(jwkProviderUrl).body()
    }
}