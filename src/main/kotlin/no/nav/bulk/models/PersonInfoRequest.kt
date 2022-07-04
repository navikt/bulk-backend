package no.nav.bulk.models

@kotlinx.serialization.Serializable
data class PersonInfoRequest(val personidenter: List<String>)