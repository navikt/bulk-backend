package no.nav.bulk.models


@kotlinx.serialization.Serializable
data class DigDirRequest(val personidenter: List<String>)

@kotlinx.serialization.Serializable
data class DigDirPersonInfo(
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
 * Enum class denoting the different errors that DigDir can return.
 */
@kotlinx.serialization.Serializable
enum class DigDirFeil(val value: String) {
    PERSON_IKKE_FUNNET("person_ikke_funnet"),
    STRENGT_FORTROLIG_ADRESSE("strengt_fortrolig_adresse"),
    STRENGT_FORTROLIG_UTENLANDSK_ADRESSE("strengt_fortrolig_utenlandsk_adresse"),
    FORTROLIG_ADRESSE("fortrolig_adresse"),
    SKJERMET("skjermet")
}
/**
 * personer maps the person with the personident to the corresponding personinfo object
 * feil maps the personident from a PersonInfo object, into a String that contains the error message
 *
 * SikkerDigitalPostkasse
 * @param SikkerDigitalPostkasse
 */
@kotlinx.serialization.Serializable
data class DigDirResponse(val personer: Map<String, DigDirPersonInfo>, val feil: Map<String, DigDirFeil>)

@kotlinx.serialization.Serializable
data class SikkerDigitalPostkasse(
    val adresse: String? = null,
    val leverandoerAdresse: String? = null,
    val leverandoerSertifikat: String? = null
)

