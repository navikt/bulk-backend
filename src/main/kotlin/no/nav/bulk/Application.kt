package no.nav.bulk

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.bulk.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureHTTP()
    }.start(wait = true)
}
