package no.nav.bulk.lib

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import no.nav.bulk.client
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
    println(response.status)
    return response.body()
}



suspend fun getContactInfo(personnr: List<String>) {
    val accessToken = getAccessToken()
    println(accessToken)
//    client.post(Urls.DIGDIR_KRR_API_URL) {
//        headers {
//            append("Authorization", "Bearer ${accessToken.access_token}")
//            append("Nav-Call-Id", UUID.randomUUID().toString())
//        }
//        setBody(
//            buildJsonObject {
//                put("personidenter", buildJsonArray { personnr })
//            }
//        )
//    }
}
