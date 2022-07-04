package no.nav.bulk.models


@kotlinx.serialization.Serializable
data class ContactInfoRequest(val personidenter: List<String>)