package no.nav.bulk.integrationtests

import io.github.cdimascio.dotenv.dotenv

val dotenv = dotenv {
    ignoreIfMissing = true
}

val MOCK_AUTH_JSON_CONFIG: String = dotenv["JSON_CONFIG"]