package no.nav.bulk.lib

import no.nav.bulk.models.*
import java.time.ZonedDateTime

object FilterTestData {

    private val DEFAULT_PERSON_INPUT = KRRAPIPerson(
        personident = "1234",
        aktiv = true,
        kanVarsles = true,
        reservert = false,
        spraak = "nb",
        epostadresse = "ola@nordmann.no",
        epostadresseOppdatert = ZonedDateTime.now().toString(),
        mobiltelefonnummer = "12345678",
        mobiltelefonnummerOppdatert = ZonedDateTime.now().toString(),
        sikkerDigitalPostkasse = SikkerDigitalPostkasse(
            adresse = "Nordmannveien 12A",
            leverandoerAdresse = "Digipost",
            leverandoerSertifikat = "noe"
        )
    )

    private val DEFAULT_PERSON_RESULT = MappedKRRPerson(
        personident = "1234",
        spraak = "nb",
        epostadresse = "ola@nordmann.no",
        mobiltelefonnummer = "12345678",
        adresse = "Nordmannveien 12A",
        leverandoerAdresse = "Digipost",
        leverandoerSertifikat = "noe"
    )

    private fun createInput(person: KRRAPIPerson? = null, feil: KRRAPIError? = null) = KRRAPIResponse(
        personer = if (person == null) emptyMap() else mapOf(
            "1234" to person
        ),
        feil = if (feil == null) emptyMap() else mapOf(
            "1234" to feil
        )
    )

    private fun createPersonData(person: MappedKRRPerson?, feil: ErrorType? = null) =
        MappedKRRPersonResponse(person = person, feil = feil)

    private fun createResult(personData: MappedKRRPersonResponse) = MappedKRRResponse(
        personer = mapOf("1234" to personData)
    )

    val INPUT_KAN_VARSLES_TRUE = createInput(DEFAULT_PERSON_INPUT)

    val RESULT_KAN_VALRSLES_TRUE = createResult(createPersonData(DEFAULT_PERSON_RESULT))

    val INPUT_KAN_VARSLES_FALSE = createInput(
        DEFAULT_PERSON_INPUT.copy(
            kanVarsles = false,
            reservert = true,
        )
    )

    val RESULT_KAN_VALRSLES_FALSE = createResult(
        createPersonData(
            null,
            ErrorType.KAN_IKKE_VARSLES
        )
    )

    val INPUT_OUTDATED_CONTACT_INFO = createInput(
        DEFAULT_PERSON_INPUT.copy(
            epostadresseOppdatert = ZonedDateTime.now().minusMonths(18).toString(),
            mobiltelefonnummerOppdatert = ZonedDateTime.now().minusMonths(18).toString(),
        )
    )

    val RESULT_OUTDATED_CONTACT_INFO = createResult(
        createPersonData(
            person = null,
            feil = ErrorType.UTDATERT_KONTAKTINFORMASJON
        )
    )


    val INPUT_OUTDATED_EPOST_NOT_NUMBER = createInput(
        DEFAULT_PERSON_INPUT.copy(
            epostadresseOppdatert = ZonedDateTime.now().minusMonths(18).toString(),
        )
    )

    val RESULT_OUTDATED_EPOST_NOT_NUMBER = createResult(
        createPersonData(
            person = DEFAULT_PERSON_RESULT.copy(epostadresse = null)
        )
    )

    val INPUT_OUTDATED_NUMBER_NOT_EPOST = createInput(
        DEFAULT_PERSON_INPUT.copy(
            mobiltelefonnummerOppdatert = ZonedDateTime.now().minusMonths(18).toString(),
        )
    )

    val RESULT_OUTDATED_NUMBER_NOT_EPOST = createResult(
        createPersonData(DEFAULT_PERSON_RESULT.copy(mobiltelefonnummer = null))
    )

    val INPUT_OUTDATED_NUMBER_EPOST_NULL = createInput(
        DEFAULT_PERSON_INPUT.copy(
            mobiltelefonnummerOppdatert = ZonedDateTime.now().minusMonths(18).toString(),
            epostadresse = null
        )
    )

    val RESULT_OUTDATED_NUMBER_EPOST_NULL = createResult(
        createPersonData(null, ErrorType.UTDATERT_KONTAKTINFORMASJON)
    )

    val INPUT_OUTDATED_EPOST_NUMBER_NULL = createInput(
        DEFAULT_PERSON_INPUT.copy(
            epostadresseOppdatert = ZonedDateTime.now().minusMonths(18).toString(),
            mobiltelefonnummer = null
        )
    )

    val RESULT_OUTDATED_EPOST_NUMBER_NULL = createResult(
        createPersonData(null, ErrorType.UTDATERT_KONTAKTINFORMASJON)
    )

    val INPUT_EPOST_NUMBER_NULL = createInput(
        DEFAULT_PERSON_INPUT.copy(
            epostadresse = null,
            mobiltelefonnummer = null
        )
    )

    val RESULT_EPOST_NUMBER_NULL = createResult(
        createPersonData(null, ErrorType.KAN_IKKE_VARSLES)
    )

    val INPUT_FEIL_PERSON_IKKE_FUNNET = createInput(null, KRRAPIError.PERSON_IKKE_FUNNET)

    val RESULT_FEIL_PERSON_IKKE_FUNNET = createResult(
        createPersonData(
            person = null,
            feil = ErrorType.PERSON_IKKE_FUNNET
        )
    )

    val INPUT_FEIL_STRENGT_FORTROLIG_ADRESSE = createInput(null, KRRAPIError.STRENGT_FORTROLIG_ADRESSE)

    val RESULT_FEIL_STRENGT_FORTROLIG_ADRESSE = createResult(
        createPersonData(
            person = null,
            feil = ErrorType.STRENGT_FORTROLIG_ADRESSE
        )
    )

    val INPUT_FEIL_STRENGT_FORTROLIG_UTENLANDSK_ADRESSE =
        createInput(null, KRRAPIError.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE)

    val RESULT_FEIL_STRENGT_FORTROLIG_UTENLANDSK_ADRESSE = createResult(
        createPersonData(
            person = null,
            feil = ErrorType.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE
        )
    )

    val INPUT_FEIL_FORTROLIG_ADRESSE = createInput(null, KRRAPIError.FORTROLIG_ADRESSE)

    val RESULT_FEIL_FORTROLIG_ADRESSE = createResult(
        createPersonData(
            person = null,
            feil = ErrorType.FORTROLIG_ADRESSE
        )
    )

    val INPUT_FEIL_SKJERMET = createInput(null, KRRAPIError.SKJERMET)

    val RESULT_FEIL_SKJERMET = createResult(
        createPersonData(
            person = null,
            feil = ErrorType.SKJERMET
        )
    )

    val INPUT_MULTIPLE_PEOPLE = KRRAPIResponse(
        personer = mapOf(
            "1234" to DEFAULT_PERSON_INPUT,
            "4321" to DEFAULT_PERSON_INPUT.copy(personident = "4321", kanVarsles = false, reservert = true),
            "6432" to DEFAULT_PERSON_INPUT.copy(personident = "6432", kanVarsles = false, reservert = true),
            "2345" to DEFAULT_PERSON_INPUT.copy(
                personident = "2345",
                epostadresseOppdatert = ZonedDateTime.now().minusMonths(18).toString()
            ),
            "5678" to DEFAULT_PERSON_INPUT.copy(
                personident = "5678",
                mobiltelefonnummerOppdatert = ZonedDateTime.now().minusMonths(18).toString()
            ),
            "7890" to DEFAULT_PERSON_INPUT.copy(
                personident = "7890",
                mobiltelefonnummerOppdatert = ZonedDateTime.now().minusMonths(18).toString(),
                epostadresseOppdatert = ZonedDateTime.now().minusMonths(18).toString()
            ),
        ),
        feil = mapOf(
            "1111" to KRRAPIError.PERSON_IKKE_FUNNET,
            "2222" to KRRAPIError.SKJERMET,
            "3333" to KRRAPIError.SKJERMET
        )
    )

    val RESULT_MULTIPLE_PEOPLE = MappedKRRResponse(
        personer = mapOf(
            "1234" to MappedKRRPersonResponse(DEFAULT_PERSON_RESULT, null),
            "4321" to MappedKRRPersonResponse(null, ErrorType.KAN_IKKE_VARSLES),
            "6432" to MappedKRRPersonResponse(null, ErrorType.KAN_IKKE_VARSLES),
            "2345" to MappedKRRPersonResponse(
                DEFAULT_PERSON_RESULT.copy(personident = "2345", epostadresse = null),
                null
            ),
            "5678" to MappedKRRPersonResponse(
                DEFAULT_PERSON_RESULT.copy(
                    personident = "5678",
                    mobiltelefonnummer = null
                ), null
            ),
            "7890" to MappedKRRPersonResponse(null, ErrorType.UTDATERT_KONTAKTINFORMASJON),
            "1111" to MappedKRRPersonResponse(null, ErrorType.PERSON_IKKE_FUNNET),
            "2222" to MappedKRRPersonResponse(null, ErrorType.SKJERMET),
            "3333" to MappedKRRPersonResponse(null, ErrorType.SKJERMET),
        )
    )
}