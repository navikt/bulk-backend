package no.nav.bulk.models

/**
 * The expected request class to this API.
 */
@kotlinx.serialization.Serializable
data class PeopleDataRequest(val personidenter: List<String>)

/**
 * The relevant part of PersonInfo (data from KRR) that is sent as a response from this API.
 */
data class Person(
    val personident: String,
    val spraak: String? = null,
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null,
    val adresse: String? = null,
    val leverandoerAdresse: String? = null,
    val leverandoerSertifikat: String? = null,
)

enum class FeilType(val value: String) {
    RESERVERT("reservert"),
    KAN_IKKE_VARSLES("kan_ikke_varsles"),
    PERSON_IKKE_FUNNET("person_ikke_funnet"),
    UTDATERT_KONTAKTINFORMASJON("utdatert_kontaktinformasjon"),
    STRENGT_FORTROLIG_ADRESSE("strengt_fortrolig_adresse"),
    STRENGT_FORTROLIG_UTENLANDSK_ADRESSE("strengt_fortrolig_utenlandsk_adresse"),
    FORTROLIG_ADRESSE("fortrolig_adresse"),
    SKJERMET("skjermet"),
}

/**
 * Container class consisting of the Person object if there was no error, or an error object
 * denoting if that person is not available.
 */
data class PersonData(val person: Person?, val feil: FeilType?)

/**
 * The actual response sent back to the user from this API.
 * The string in the hashmap maps to the personident in the person object.
 */
data class PeopleDataResponse(val personer: Map<String, PersonData>)
