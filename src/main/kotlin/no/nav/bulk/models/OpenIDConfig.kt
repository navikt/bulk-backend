package no.nav.bulk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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