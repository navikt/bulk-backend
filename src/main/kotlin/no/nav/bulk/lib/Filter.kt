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

sealed class ParseResult {
    class Success(val caveat: SuccessCaveat) : ParseResult()
    class Fail(val feilType: FeilType) : ParseResult()
}

fun isValidDate(dateString: String?): Boolean {
    val now = LocalDateTime.now()
    val date = if (dateString != null) LocalDateTime.parse(dateString) else null
    return if (date != null) now.minusMonths(18).isBefore(date) else false
}

fun isValidContactInfo(personInfo: DigDirPersonInfo): ContactInfoStatus {
    val invalidEmail = !isValidDate(personInfo.epostadresseOppdatert)
    val invalidPhone = !isValidDate(personInfo.mobiltelefonnummerOppdatert)
    if (invalidEmail && invalidPhone) return ContactInfoStatus.BOTH_OUTDATED
    if (invalidEmail && personInfo.mobiltelefonnummer == null) return ContactInfoStatus.BOTH_OUTDATED
    if (invalidPhone && personInfo.epostadresse == null) return ContactInfoStatus.BOTH_OUTDATED
    if (invalidEmail) return ContactInfoStatus.OUTDATED_EMAIL
    if (invalidPhone) return ContactInfoStatus.OUTDATED_PHONE
    return ContactInfoStatus.OK
}

fun insideForLoop(personInfo: DigDirPersonInfo): ParseResult {


    // When person is "null" in DigDir, it means that the person is not allowed to be contacted
    if (personInfo.kanVarsles == null || personInfo.kanVarsles == false) {
        return ParseResult.Fail(FeilType.RESERVERT)
    }
    // check contact info (email and phone)
    return when (isValidContactInfo(personInfo)) {
        ContactInfoStatus.OK -> ParseResult.Success(SuccessCaveat.NONE)
        ContactInfoStatus.BOTH_OUTDATED -> ParseResult.Fail(FeilType.UTDATERT_KONTAKTINFORMASJON)
        ContactInfoStatus.OUTDATED_EMAIL -> ParseResult.Success(SuccessCaveat.OUTDATED_EMAIL)
        ContactInfoStatus.OUTDATED_PHONE -> ParseResult.Success(SuccessCaveat.OUTDATED_PHONE)
    }

    // test outdated contact info
}

fun successMap(personInfo: DigDirPersonInfo, caveat: SuccessCaveat): PersonData {
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

fun filterAndMapDigDirResponse(digDirResponse: DigDirResponse): PersonInfoResponse {
    val personResponseMap = mutableMapOf<String, PersonData>()

    for ((personident, personInfo) in digDirResponse.personer) {
        when (val result = insideForLoop(personInfo)) {
            is ParseResult.Fail -> personResponseMap[personident] = PersonData(null, result.feilType)
            is ParseResult.Success -> personResponseMap[personident] = successMap(personInfo, result.caveat)
        }
    }
    for ((personident, feil) in digDirResponse.feil) {
        when (DigDirFeil.valueOf(feil.uppercase())) {
            DigDirFeil.PERSON_IKKE_FUNNET -> personResponseMap[personident] =
                PersonData(null, FeilType.PERSON_IKKE_FUNNET)
            DigDirFeil.SKJERMET -> personResponseMap[personident] =
                PersonData(null, FeilType.SKJERMET)
            DigDirFeil.STRENGT_FORTROLIG_ADRESSE -> personResponseMap[personident] =
                PersonData(null, FeilType.STRENGT_FORTROLIG_ADRESSE)
            DigDirFeil.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE -> personResponseMap[personident] =
                PersonData(null, FeilType.STRENGT_FORTROLIG_UTENLANDSK_ADRESSE)
            DigDirFeil.FORTROLIG_ADRESSE -> personResponseMap[personident] =
                PersonData(null, FeilType.FORTROLIG_ADRESSE)
        }
    }
    return PersonInfoResponse(personResponseMap)
}


