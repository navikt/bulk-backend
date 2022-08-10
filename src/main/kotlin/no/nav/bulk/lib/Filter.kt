package no.nav.bulk.lib

import no.nav.bulk.models.*
import java.time.ZonedDateTime

enum class SuccessCaveat {
    NONE,
    INVALID_EMAIL,
    INVALID_PHONE
}

sealed class KRRAPIPersonValidationResult {
    class Success(val caveat: SuccessCaveat) : KRRAPIPersonValidationResult()
    class Fail(val feilType: ErrorType) : KRRAPIPersonValidationResult()
}

fun isValidUpdatedDate(dateContactInfoUpdated: String?): Boolean {
    val now = ZonedDateTime.now()
    val date = if (dateContactInfoUpdated != null) ZonedDateTime.parse(dateContactInfoUpdated) else return false
    return now.minusMonths(18).isBefore(date)
}

fun isValidContactInfo(person: KRRAPIPerson): KRRAPIPersonValidationResult {
    val oudatedEmail = !isValidUpdatedDate(person.epostadresseOppdatert)
    val outdatedPhone = !isValidUpdatedDate(person.mobiltelefonnummerOppdatert)
    val nullEmail = person.epostadresse == null
    val nullPhone = person.mobiltelefonnummer == null
    if (oudatedEmail && (outdatedPhone || nullPhone)) return KRRAPIPersonValidationResult.Fail(ErrorType.UTDATERT_KONTAKTINFORMASJON)
    if (outdatedPhone && nullEmail) return KRRAPIPersonValidationResult.Fail(ErrorType.UTDATERT_KONTAKTINFORMASJON)
    if (nullEmail && nullPhone) return KRRAPIPersonValidationResult.Fail(ErrorType.KAN_IKKE_VARSLES)
    if (oudatedEmail || nullEmail) return KRRAPIPersonValidationResult.Success(SuccessCaveat.INVALID_EMAIL)
    if (outdatedPhone || nullPhone) return KRRAPIPersonValidationResult.Success(SuccessCaveat.INVALID_PHONE)
    if (person.kanVarsles != true) return KRRAPIPersonValidationResult.Fail(ErrorType.KAN_IKKE_VARSLES)
    return KRRAPIPersonValidationResult.Success(SuccessCaveat.NONE)
}

fun validateKRRAPIPerson(person: KRRAPIPerson): KRRAPIPersonValidationResult {
    // When person is "null" in DigDir, it means that the person is not allowed to be contacted
    if (person.reservert != false || person.aktiv != true) {
        return KRRAPIPersonValidationResult.Fail(ErrorType.KAN_IKKE_VARSLES)
    }
    return isValidContactInfo(person)
}

fun mapToPersonResponse(person: KRRAPIPerson, caveat: SuccessCaveat): MappedKRRPersonResponse {
    return MappedKRRPersonResponse(
        MappedKRRPerson(
            personident = person.personident,
            spraak = person.spraak,
            epostadresse = if (caveat == SuccessCaveat.INVALID_EMAIL) null else person.epostadresse,
            mobiltelefonnummer = if (caveat == SuccessCaveat.INVALID_PHONE) null else person.mobiltelefonnummer,
            adresse = person.sikkerDigitalPostkasse?.adresse,
            leverandoerAdresse = person.sikkerDigitalPostkasse?.leverandoerAdresse,
            leverandoerSertifikat = person.sikkerDigitalPostkasse?.leverandoerSertifikat
        ), null
    )
}

fun filerAndMapKRRResponse(krrApiResponse: KRRAPIResponse): MappedKRRResponse {
    val peopleResponseMap = mutableMapOf<String, MappedKRRPersonResponse>()

    for ((personident, personInfo) in krrApiResponse.personer) {
        when (val result = validateKRRAPIPerson(personInfo)) {
            is KRRAPIPersonValidationResult.Fail -> peopleResponseMap[personident] =
                MappedKRRPersonResponse(null, result.feilType)

            is KRRAPIPersonValidationResult.Success -> peopleResponseMap[personident] =
                mapToPersonResponse(personInfo, result.caveat)
        }
    }
    for ((personident, feil) in krrApiResponse.feil) {
        peopleResponseMap[personident] = MappedKRRPersonResponse(null, ErrorType.valueOf(feil.name))
    }
    return MappedKRRResponse(peopleResponseMap)
}


