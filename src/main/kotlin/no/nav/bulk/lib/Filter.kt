package no.nav.bulk.lib

import no.nav.bulk.models.*
import java.time.ZonedDateTime

enum class SuccessCaveat {
    NONE,
    OUTDATED_EMAIL,
    OUTDATED_PHONE
}

enum class ContactInfoStatus {
    BOTH_INVALID,
    INVALID_EMAIL,
    INVALID_PHONE,
    NO_CONTACT_INFO,
    OK
}

sealed class DigDirPersonValidationResult {
    class Success(val caveat: SuccessCaveat) : DigDirPersonValidationResult()
    class Fail(val feilType: FeilType) : DigDirPersonValidationResult()
}

fun isValidContactInfo(contactInfo: String?, dateContactInfoUpdated: String?): Boolean {
    if (contactInfo == null) return false
    val now = ZonedDateTime.now()
    val date = if (dateContactInfoUpdated != null) ZonedDateTime.parse(dateContactInfoUpdated) else return false
    return now.minusMonths(18).isBefore(date)
}

fun isValidContactInfo(personInfo: DigDirPerson): ContactInfoStatus {
    val invalidEmail = !isValidContactInfo(personInfo.epostadresse, personInfo.epostadresseOppdatert)
    val invalidPhone = !isValidContactInfo(personInfo.mobiltelefonnummer, personInfo.mobiltelefonnummerOppdatert)
    if (invalidEmail && invalidPhone) return ContactInfoStatus.BOTH_INVALID
    if (invalidEmail && personInfo.mobiltelefonnummer == null) return ContactInfoStatus.BOTH_INVALID
    if (invalidPhone && personInfo.epostadresse == null) return ContactInfoStatus.BOTH_INVALID
    if (personInfo.epostadresse == null && personInfo.mobiltelefonnummer == null) return ContactInfoStatus.NO_CONTACT_INFO
    if (invalidEmail) return ContactInfoStatus.INVALID_EMAIL
    if (invalidPhone) return ContactInfoStatus.INVALID_PHONE
    return ContactInfoStatus.OK
}

fun validateDigDirPersonInfo(personInfo: DigDirPerson): DigDirPersonValidationResult {
    // When person is "null" in DigDir, it means that the person is not allowed to be contacted
    if (personInfo.kanVarsles != true || personInfo.reservert != false || personInfo.aktiv != true) {
        return DigDirPersonValidationResult.Fail(FeilType.KAN_IKKE_VARSLES)
    }
    // check contact info (email and phone)
    return when (isValidContactInfo(personInfo)) {
        ContactInfoStatus.OK -> DigDirPersonValidationResult.Success(SuccessCaveat.NONE)
        ContactInfoStatus.BOTH_INVALID -> DigDirPersonValidationResult.Fail(FeilType.UTDATERT_KONTAKTINFORMASJON)
        ContactInfoStatus.INVALID_EMAIL -> DigDirPersonValidationResult.Success(SuccessCaveat.OUTDATED_EMAIL)
        ContactInfoStatus.INVALID_PHONE -> DigDirPersonValidationResult.Success(SuccessCaveat.OUTDATED_PHONE)
        ContactInfoStatus.NO_CONTACT_INFO -> DigDirPersonValidationResult.Fail(FeilType.KAN_IKKE_VARSLES)
    }
}

fun mapToPersonData(personInfo: DigDirPerson, caveat: SuccessCaveat): PersonData {
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
        val result = validateDigDirPersonInfo(personInfo)
        if (result is DigDirPersonValidationResult.Fail) println(result.feilType)
        when (result) {
            is DigDirPersonValidationResult.Fail -> peopleResponseMap[personident] = PersonData(null, result.feilType)
            is DigDirPersonValidationResult.Success -> peopleResponseMap[personident] =
                mapToPersonData(personInfo, result.caveat)
        }
    }
    for ((personident, feil) in digDirResponse.feil) {
        when (DigDirFeil.valueOf(feil.name)) {
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


