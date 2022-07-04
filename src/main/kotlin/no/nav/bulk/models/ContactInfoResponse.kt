package no.nav.bulk.models

@kotlinx.serialization.Serializable
data class PersonInfo(
    val personident: String,
    val aktiv: Boolean? = null,
    val kanVarsles: Boolean? = null,
    val reservert: Boolean? = null,
    val spraak: String? = null,
    val epostadresse: String? = null,
    val epostadresseOppdatert: String? = null,
    val mobiltelefonnummer: String? = null,
    val mobiltelefonnummerOppdatert: String? = null,
    val sikkerDigitalPostkasse: SikkerDigitalPostkasse? = null
)

/**
 * personer maps the person with the personident to the corresponding personinfo object
 * feil maps the personident from a PersonInfo object, into a String that contains the error message
 *
 * SikkerDigitalPostkasse
 * @param SikkerDigitalPostkasse
 */
@kotlinx.serialization.Serializable
data class ContactInfoResponse(val personer: Map<String, PersonInfo>, val feil: Map<String, String>)

@kotlinx.serialization.Serializable
data class SikkerDigitalPostkasse(
    val adresse: String? = null,
    val leverandoerAdresse: String? = null,
    val leverandoerSertifikat: String? = null
)