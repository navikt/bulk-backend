package no.nav.bulk.lib

import no.nav.bulk.models.*
import java.time.LocalDateTime

enum class SuccessCaveat {
    NONE,
    OUTDATED_EMAIL,
    OUTDATED_PHONE
}

enum class ContactInfoStatus {
    BOTH_OUTDATED,
    OUTDATED_EMAIL,
    OUTDATED_PHONE,
    OK
}

sealed class DigDirPersonValidationResult {
    class Success(val caveat: SuccessCaveat) : DigDirPersonValidationResult()
    class Fail(val feilType: FeilType) : DigDirPersonValidationResult()
}

fun isValidDate(dateString: String?): Boolean {
    val now = LocalDateTime.now()
    val date = if (dateString != null) LocalDateTime.parse(dateString) else null
    return if (date != null) now.minusMonths(18).isBefore(date) else false
}

fun isValidContactInfo(personInfo: DigDirPerson): ContactInfoStatus {
    val invalidEmail = !isValidDate(personInfo.epostadresseOppdatert)
    val invalidPhone = !isValidDate(personInfo.mobiltelefonnummerOppdatert)
    if (invalidEmail && invalidPhone) return ContactInfoStatus.BOTH_OUTDATED
    if (invalidEmail && personInfo.mobiltelefonnummer == null) return ContactInfoStatus.BOTH_OUTDATED
    if (invalidPhone && personInfo.epostadresse == null) return ContactInfoStatus.BOTH_OUTDATED
    if (invalidEmail) return ContactInfoStatus.OUTDATED_EMAIL
    if (invalidPhone) return ContactInfoStatus.OUTDATED_PHONE
    return ContactInfoStatus.OK
}

fun validateDigDirPersonInfo(personInfo: DigDirPerson): DigDirPersonValidationResult {


    // When person is "null" in DigDir, it means that the person is not allowed to be contacted
    if (personInfo.kanVarsles == null || personInfo.kanVarsles == false) {
        return DigDirPersonValidationResult.Fail(FeilType.RESERVERT)
    }
    // check contact info (email and phone)
    return when (isValidContactInfo(personInfo)) {
        ContactInfoStatus.OK -> DigDirPersonValidationResult.Success(SuccessCaveat.NONE)
        ContactInfoStatus.BOTH_OUTDATED -> DigDirPersonValidationResult.Fail(FeilType.UTDATERT_KONTAKTINFORMASJON)
        ContactInfoStatus.OUTDATED_EMAIL -> DigDirPersonValidationResult.Success(SuccessCaveat.OUTDATED_EMAIL)
        ContactInfoStatus.OUTDATED_PHONE -> DigDirPersonValidationResult.Success(SuccessCaveat.OUTDATED_PHONE)
    }

    // test outdated contact info
}

fun successMap(personInfo: DigDirPerson, caveat: SuccessCaveat): PersonData {
    return PersonData(
        Person(
            personident = personInfo.personident,
            spraak = personInfo.spraak,
            epostadresse = if (caveat == SuccessCaveat.OUTDATED_EMAIL) null else personInfo.epostadresse,
            mobiltelefonnummer = if (caveat == SuccessCaveat.OUTDATED_PHONE) null else personInfo.mobiltelefonnummer,
            adresse = personInfo.sikkerDigitalPostkasse?.adresse,
            leverandoerAdresse = personInfo.sikkerDigitalPostkasse?.leverandoerAdresse,
            leverandoerSertifikat = personInfo.sikkerDigitalPostkasse?.leverandoerSertifikat
        ), null
    )
}

fun filterAndMapDigDirResponse(digDirResponse: DigDirResponse): PeopleDataResponse {
    val peopleResponseMap = mutableMapOf<String, PersonData>()

    for ((personident, personInfo) in digDirResponse.personer) {
        when (val result = validateDigDirPersonInfo(personInfo)) {
            is DigDirPersonValidationResult.Fail -> peopleResponseMap[personident] = PersonData(null, result.feilType)
            is DigDirPersonValidationResult.Success -> peopleResponseMap[personident] =
                successMap(personInfo, result.caveat)
        }
    }
    for ((personident, feil) in digDirResponse.feil) {
        when (DigDirFeil.valueOf(feil.uppercase())) {
            DigDirFeil.PERSON_IKKE_FUNNET -> peopleResponseMap[personident] =
                PersonData(null, FeilType.PERSON_IKKE_FUNNET)
            DigDirFeil.SKJERMET -> peopleResponseMap[personident] =
                PersonData(null, FeilType.SKJERMET)
            DigDirFeil.STRENGT_FORTROLIG_ADRESSE -> peopleResponseMap[personident] =
                PersonData(null, FeilType.STRENGT_FORTROLIG_ADRESSE)
            DigDirFeil.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE -> peopleResponseMap[personident] =
                PersonData(null, FeilType.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE)
            DigDirFeil.FORTROLIG_ADRESSE -> peopleResponseMap[personident] =
                PersonData(null, FeilType.FORTROLIG_ADRESSE)
        }
    }
    return PeopleDataResponse(peopleResponseMap)
}


