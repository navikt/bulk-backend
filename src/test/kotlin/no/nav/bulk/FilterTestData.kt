package no.nav.bulk

import no.nav.bulk.models.*
import java.time.LocalDateTime

object FilterTestData {

    private val DEFAULT_PERSON_INPUT = DigDirPerson(
        personident = "1234",
        aktiv = true,
        kanVarsles = true,
        reservert = false,
        spraak = "nb",
        epostadresse = "ola@nordmann.no",
        epostadresseOppdatert = LocalDateTime.now().toString(),
        mobiltelefonnummer = "12345678",
        mobiltelefonnummerOppdatert = LocalDateTime.now().toString(),
        sikkerDigitalPostkasse = SikkerDigitalPostkasse(
            adresse = "Nordmannveien 12A",
            leverandoerAdresse = "Digipost",
            leverandoerSertifikat = "noe"
        )
    )

    private val DEFAULT_PERSON_RESULT = Person(
        personident = "1234",
        spraak = "nb",
        epostadresse = "ola@nordmann.no",
        mobiltelefonnummer = "12345678",
        adresse = "Nordmannveien 12A",
        leverandoerAdresse = "Digipost",
        leverandoerSertifikat = "noe"
    )

    private fun buildInput(person: DigDirPerson? = null, feil: DigDirFeil? = null) = DigDirResponse(
        personer = if (person == null) emptyMap() else mapOf(
            "1234" to person
        ),
        feil = if (feil == null) emptyMap() else mapOf(
            "1234" to feil.value
        )
    )

    private fun buildPersonData(person: Person?, feil: FeilType? = null) = PersonData(person = person, feil = feil)

    private fun buildResult(personData: PersonData) = PeopleDataResponse(
        personer = mapOf("1234" to personData)
    )

    val INPUT_KAN_VARSLES_TRUE = buildInput(DEFAULT_PERSON_INPUT)

    val RESULT_KAN_VALRSLES_TRUE = buildResult(buildPersonData(DEFAULT_PERSON_RESULT))

    val INPUT_KAN_VARSLES_FALSE = buildInput(
        DEFAULT_PERSON_INPUT.copy(
            kanVarsles = false,
            reservert = true,
        )
    )

    val RESULT_KAN_VALRSLES_FALSE = buildResult(
        buildPersonData(
            null,
            FeilType.RESERVERT
        )
    )

    val INPUT_OUTDATED_CONTACT_INFO = buildInput(
        DEFAULT_PERSON_INPUT.copy(
            epostadresseOppdatert = LocalDateTime.now().minusMonths(18).toString(),
            mobiltelefonnummerOppdatert = LocalDateTime.now().minusMonths(18).toString(),
        )
    )

    val RESULT_OUTDATED_CONTACT_INFO = buildResult(
        buildPersonData(
            person = null,
            feil = FeilType.UTDATERT_KONTAKTINFORMASJON
        )
    )


    val INPUT_OUTDATED_EPOST_NOT_NUMBER = buildInput(
        DEFAULT_PERSON_INPUT.copy(
            epostadresseOppdatert = LocalDateTime.now().minusMonths(18).toString(),
        )
    )

    val RESULT_OUTDATED_EPOST_NOT_NUMBER = buildResult(
        buildPersonData(
            person = DEFAULT_PERSON_RESULT.copy(epostadresse = null)
        )
    )

    val INPUT_OUTDATED_NUMBER_NOT_EPOST = buildInput(
        DEFAULT_PERSON_INPUT.copy(
            mobiltelefonnummerOppdatert = LocalDateTime.now().minusMonths(18).toString(),
        )
    )

    val RESULT_OUTDATED_NUMBER_NOT_EPOST = buildResult(
        buildPersonData(DEFAULT_PERSON_RESULT.copy(mobiltelefonnummer = null))
    )

    val INPUT_OUTDATED_NUMBER_EPOST_NULL = buildInput(
        DEFAULT_PERSON_INPUT.copy(
            mobiltelefonnummerOppdatert = LocalDateTime.now().minusMonths(18).toString(),
            epostadresse = null
        )
    )

    val RESULT_OUTDATED_NUMBER_EPOST_NULL = buildResult(
        buildPersonData(null, FeilType.UTDATERT_KONTAKTINFORMASJON)
    )

    val INPUT_OUTDATED_EPOST_NUMBER_NULL = buildInput(
        DEFAULT_PERSON_INPUT.copy(
            epostadresseOppdatert = LocalDateTime.now().minusMonths(18).toString(),
            mobiltelefonnummer = null
        )
    )

    val RESULT_OUTDATED_EPOST_NUMBER_NULL = buildResult(
        buildPersonData(null, FeilType.UTDATERT_KONTAKTINFORMASJON)
    )

    val INPUT_FEIL_PERSON_IKKE_FUNNET = buildInput(null, DigDirFeil.PERSON_IKKE_FUNNET)

    val RESULT_FEIL_PERSON_IKKE_FUNNET = buildResult(
        buildPersonData(
            person = null,
            feil = FeilType.PERSON_IKKE_FUNNET
        )
    )

    val INPUT_FEIL_STRENGT_FORTROLIG_ADRESSE = buildInput(null, DigDirFeil.STRENGT_FORTROLIG_ADRESSE)

    val RESULT_FEIL_STRENGT_FORTROLIG_ADRESSE = buildResult(
        buildPersonData(
            person = null,
            feil = FeilType.STRENGT_FORTROLIG_ADRESSE
        )
    )

    val INPUT_FEIL_STRENGT_FORTROLIG_UTENLANDSK_ADRESSE =
        buildInput(null, DigDirFeil.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE)

    val RESULT_FEIL_STRENGT_FORTROLIG_UTENLANDSK_ADRESSE = buildResult(
        buildPersonData(
            person = null,
            feil = FeilType.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE
        )
    )

    val INPUT_FEIL_FORTROLIG_ADRESSE = buildInput(null, DigDirFeil.FORTROLIG_ADRESSE)

    val RESULT_FEIL_FORTROLIG_ADRESSE = buildResult(
        buildPersonData(
            person = null,
            feil = FeilType.FORTROLIG_ADRESSE
        )
    )

    val INPUT_FEIL_SKJERMET = buildInput(null, DigDirFeil.SKJERMET)

    val RESULT_FEIL_SKJERMET = buildResult(
        buildPersonData(
            person = null,
            feil = FeilType.SKJERMET
        )
    )

    val INPUT_MULTIPLE_PEOPLE = DigDirResponse(
        personer = mapOf(
            "1234" to DEFAULT_PERSON_INPUT,
            "4321" to DEFAULT_PERSON_INPUT.copy(personident = "4321", kanVarsles = false, reservert = true),
            "6432" to DEFAULT_PERSON_INPUT.copy(personident = "6432", kanVarsles = false, reservert = true),
            "2345" to DEFAULT_PERSON_INPUT.copy(
                personident = "2345",
                epostadresseOppdatert = LocalDateTime.now().minusMonths(18).toString()
            ),
            "5678" to DEFAULT_PERSON_INPUT.copy(
                personident = "5678",
                mobiltelefonnummerOppdatert = LocalDateTime.now().minusMonths(18).toString()
            ),
            "7890" to DEFAULT_PERSON_INPUT.copy(
                personident = "7890",
                mobiltelefonnummerOppdatert = LocalDateTime.now().minusMonths(18).toString(),
                epostadresseOppdatert = LocalDateTime.now().minusMonths(18).toString()
            ),
        ),
        feil = mapOf(
            "1111" to DigDirFeil.PERSON_IKKE_FUNNET.value,
            "2222" to DigDirFeil.SKJERMET.value,
            "3333" to DigDirFeil.SKJERMET.value
        )
    )

    val RESULT_MULTIPLE_PEOPLE = PeopleDataResponse(
        personer = mapOf(
            "1234" to PersonData(DEFAULT_PERSON_RESULT, null),
            "4321" to PersonData(null, FeilType.RESERVERT),
            "6432" to PersonData(null, FeilType.RESERVERT),
            "2345" to PersonData(DEFAULT_PERSON_RESULT.copy(personident = "2345", epostadresse = null), null),
            "5678" to PersonData(DEFAULT_PERSON_RESULT.copy(personident = "5678", mobiltelefonnummer = null), null),
            "7890" to PersonData(null, FeilType.UTDATERT_KONTAKTINFORMASJON),
            "1111" to PersonData(null, FeilType.PERSON_IKKE_FUNNET),
            "2222" to PersonData(null, FeilType.SKJERMET),
            "3333" to PersonData(null, FeilType.SKJERMET),
        )
    )
}