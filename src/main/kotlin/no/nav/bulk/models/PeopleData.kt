package no.nav.bulk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.bulk.generated.pdlquery.Bostedsadresse
import no.nav.bulk.generated.pdlquery.Doedsfall
import no.nav.bulk.generated.pdlquery.Navn

/**
 * The expected request class to this API.
 */
@Serializable
data class PersonerEndpointRequest(val personidenter: List<String>)

/**
 * The relevant part of PersonInfo (data from KRR) that is sent as a response from this API.
 */
@Serializable
data class MappedKRRPerson(
    val personident: String,
    val spraak: String? = null,
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null,
    val adresse: String? = null,
    val leverandoerAdresse: String? = null,
    val leverandoerSertifikat: String? = null,
)


@Serializable
data class PersonTotal(
    val personident: String,
    val spraak: String? = null,
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null,
    val adresse: String? = null,
    val leverandoerAdresse: String? = null,
    val leverandoerSertifikat: String? = null,
    val navn: List<Navn>? = null,
    val bostedsadresse: List<Bostedsadresse>? = null,
    val doedsfall: List<Doedsfall>? = null,
)

@Serializable
enum class ErrorType(val value: String) {
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


/**
 * Container class consisting of the Person object if there was no error, or an error object
 * denoting if that person is not available.
 */
@Serializable
data class PersonResponseGeneric<PersonType>(val person: PersonType?, val feil: ErrorType?)
typealias MappedKRRPersonResponse = PersonResponseGeneric<MappedKRRPerson>
typealias PersonResponse = PersonResponseGeneric<PersonTotal>

/**
 * The actual response sent back to the user from this API.
 * The string in the hashmap maps to the personident in the person object.
 */
@Serializable
data class PeopleDataResponseGeneric<PersonType>(val personer: Map<String, PersonType>)
typealias MappedKRRResponse = PeopleDataResponseGeneric<MappedKRRPersonResponse>
typealias PersonerEndpointResponse = PeopleDataResponseGeneric<PersonResponse>