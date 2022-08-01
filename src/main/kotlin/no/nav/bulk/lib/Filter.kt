package no.nav.bulk.lib

import no.nav.bulk.models.*
import java.time.ZonedDateTime

enum class SuccessCaveat {
    NONE,
    INVALID_EMAIL,
    INVALID_PHONE
}

sealed class DigDirPersonValidationResult {
    class Success(val caveat: SuccessCaveat) : DigDirPersonValidationResult()
    class Fail(val feilType: FeilType) : DigDirPersonValidationResult()
}

fun isValidUpdatedDate(dateContactInfoUpdated: String?): Boolean {
    val now = ZonedDateTime.now()
    val date = if (dateContactInfoUpdated != null) ZonedDateTime.parse(dateContactInfoUpdated) else return false
    return now.minusMonths(18).isBefore(date)
}

fun isValidContactInfo(personInfo: DigDirPerson): DigDirPersonValidationResult {
    val oudatedEmail = !isValidUpdatedDate(personInfo.epostadresseOppdatert)
    val outdatedPhone = !isValidUpdatedDate(personInfo.mobiltelefonnummerOppdatert)
    val nullEmail = personInfo.epostadresse == null
    val nullPhone = personInfo.mobiltelefonnummer == null
    if (oudatedEmail && outdatedPhone) return DigDirPersonValidationResult.Fail(FeilType.UTDATERT_KONTAKTINFORMASJON)
    if (oudatedEmail && nullPhone) return DigDirPersonValidationResult.Fail(FeilType.UTDATERT_KONTAKTINFORMASJON)
    if (outdatedPhone && nullEmail) return DigDirPersonValidationResult.Fail(FeilType.UTDATERT_KONTAKTINFORMASJON)
    if (nullEmail && nullPhone) return DigDirPersonValidationResult.Fail(FeilType.KAN_IKKE_VARSLES)
    if (oudatedEmail || nullEmail) return DigDirPersonValidationResult.Success(SuccessCaveat.INVALID_EMAIL)
    if (outdatedPhone || nullPhone) return DigDirPersonValidationResult.Success(SuccessCaveat.INVALID_PHONE)
    return DigDirPersonValidationResult.Success(SuccessCaveat.NONE)
}

fun validateDigDirPersonInfo(personInfo: DigDirPerson): DigDirPersonValidationResult {
    // When person is "null" in DigDir, it means that the person is not allowed to be contacted
    if (personInfo.kanVarsles != true || personInfo.reservert != false || personInfo.aktiv != true) {
        return DigDirPersonValidationResult.Fail(FeilType.KAN_IKKE_VARSLES)
    }
    return isValidContactInfo(personInfo)
}

fun mapToPersonData(personInfo: DigDirPerson, caveat: SuccessCaveat): PersonData {
    return PersonData(
        Person(
            personident = personInfo.personident,
            spraak = personInfo.spraak,
            epostadresse = if (caveat == SuccessCaveat.INVALID_EMAIL) null else personInfo.epostadresse,
            mobiltelefonnummer = if (caveat == SuccessCaveat.INVALID_PHONE) null else personInfo.mobiltelefonnummer,
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
                mapToPersonData(personInfo, result.caveat)
        }
    }
    for ((personident, feil) in digDirResponse.feil) {
        peopleResponseMap[personident] = PersonData(null, FeilType.valueOf(feil.name))
    }
    return PeopleDataResponse(peopleResponseMap)
}


