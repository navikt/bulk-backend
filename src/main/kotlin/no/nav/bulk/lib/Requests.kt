package no.nav.bulk.lib

import AuthConfig
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.bulk.client
import no.nav.bulk.models.TokenEndpointResponse

suspend fun getAccessToken(): TokenEndpointResponse {
    val response = client.post(AuthConfig.TOKEN_ENDPOINT) {



        headers {
            append(HttpHeaders.Host, "login.micorsoftonline.com")
        }
        //contentType(ContentType.Application.FormUrlEncoded)
        setBody(
            FormDataContent(Parameters.build {
                append("grant_type", "client_credentials")
                append("scope", AuthConfig.SCOPE)
                append("client_id", AuthConfig.CLIENT_ID)
                append("client_secret", AuthConfig.CLIENT_SECRET)
            })
        )
    }
    println("Headers " + response.headers.toString())
    println(response.toString())
    println(response.bodyAsText())
    return response.body()
}
