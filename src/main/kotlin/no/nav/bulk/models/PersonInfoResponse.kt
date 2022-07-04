package no.nav.bulk.models

/**
 * The relevant part of PersonInfo (data from KRR) that is sent as a response from this API.
 */
data class Person(
    val personident: String,
    val spraak: String? = null,
    val epostadresse: String? = null,
    val mobiltelefonnummer: String? = null,
    val addresse: String? = null,
    val leverandoerAdresse: String? = null,
    val leverandoerSertifikat: String? = null,
    )

/**
 * Custom error class to denote the possible errors.
 */
data class Error(val errorMessage: String?)

/**
 * Container class consisting of the Person object if there was no error, or an error object
 * denoting if that person is not available.
 */
data class PersonData(val person: Person?, val error: Error?)

/**
 * The actual response sent back to the user from this API.
 * The string in the hashmap maps to the personident in the person object.
 */
data class PersonInfoResponse(val personer: Map<String, PersonData>)
