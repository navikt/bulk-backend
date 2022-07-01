package no.nav.bulk.models

@kotlinx.serialization.Serializable
data class TokenEndpointResponse(
    val token_type: String,
    val expires_in: Int,
    val ext_expires_in: Int,
    val access_token: String
)