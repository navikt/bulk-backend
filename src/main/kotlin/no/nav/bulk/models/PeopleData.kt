package no.nav.bulk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The expected request class to this API.
 */
@Serializable
data class PeopleDataRequest(val personidenter: List<String>)

/**
 * The relevant part of PersonInfo (data from KRR) that is sent as a response from this API.
 */
@Serializable
data class Person(
    val personident: String,
    val spraak: String? = null,
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null,
    val adresse: String? = null,
    val leverandoerAdresse: String? = null,
    val leverandoerSertifikat: String? = null,
)

@Serializable
enum class FeilType(val value: String) {
    @SerialName("kan_ikke_varsles")
    KAN_IKKE_VARSLES("kan_ikke_varsles"),

    @SerialName("person_ikke_funnet")
    PERSON_IKKE_FUNNET("person_ikke_funnet"),

    @SerialName("utdatert_kontaktinformasjon")
    UTDATERT_KONTAKTINFORMASJON("utdatert_kontaktinformasjon"),

    @SerialName("strengt_fortrolig_adresse")
    STRENGT_FORTROLIG_ADRESSE("strengt_fortrolig_adresse"),

    @SerialName("strengt_fortrolig_utenlandsk_adresse")
    STRENGT_FORTROLIG_UTENLANDSK_ADRESSE("strengt_fortrolig_utenlandsk_adresse"),

    @SerialName("fortrolig_adresse")
    FORTROLIG_ADRESSE("fortrolig_adresse"),

    @SerialName("skjermet")
    SKJERMET("skjermet"),
}

fun FeilType.canQueryPdl() = this == FeilType.UTDATERT_KONTAKTINFORMASJON

/**
 * Container class consisting of the Person object if there was no error, or an error object
 * denoting if that person is not available.
 */
@Serializable
data class PersonData(val person: Person?, val feil: FeilType?)

/**
 * The actual response sent back to the user from this API.
 * The string in the hashmap maps to the personident in the person object.
 */
@Serializable
data class PeopleDataResponse(val personer: Map<String, PersonData>)
