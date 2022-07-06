package no.nav.bulk.lib

import no.nav.bulk.models.*
import java.time.LocalDateTime

enum class SucessCaveat {
    NONE,
    UTDATEREPOST,
    UTDATERTMOBILNUMMER
}

sealed class ParseResult {
    class Success(val caveat: SucessCaveat): ParseResult()
    class Fail(val feilType: FeilType): ParseResult()
}

fun parseDate(date: String?): LocalDateTime? {
    return if (date != null) LocalDateTime.parse(date) else null
}

fun isValidDate(date: LocalDateTime?): Boolean {
    val now = LocalDateTime.now()
    return if (date != null) now.minusMonths(18).isBefore(date) else false
}

fun insideForLoop(personInfo: DigDirPersonInfo): ParseResult {
    val lastUpdatedEmail = parseDate(personInfo.epostadresseOppdatert)
    val lastUpdatedPhone = parseDate(personInfo.mobiltelefonnummerOppdatert)
    // email is out of date
    if (!isValidDate(lastUpdatedEmail) && !isValidDate(lastUpdatedPhone)) {
        return ParseResult.Fail(FeilType.UTDATERT_KONTAKTINFORMASJON)
    }

    // tlf is out of date

    // When person is "null" in DigDir, it means that the person is not allowed to be contacted
    if (personInfo.kanVarsles == null || personInfo.kanVarsles == false) {
        return ParseResult.Fail(FeilType.RESERVERT)
    }

    // test outdated contact info
}

fun successMap(caveat: SucessCaveat, personInfo: DigDirPersonInfo): PersonData {
    return PersonData(null, null)
}

fun filterAndMapDigDirResponse(digDirResponse: DigDirResponse): PersonInfoResponse {
    val personResponseMap = mutableMapOf<String, PersonData>()

    for ((personident, personInfo) in digDirResponse.personer) {
        when (val result = insideForLoop(personInfo)) {
            is ParseResult.Fail -> personResponseMap[personident] = PersonData(null, result.feilType)

            is ParseResult.Success -> personResponseMap[personident] = successMap(result.caveat, personInfo)
        }

    }
    return PersonInfoResponse(personResponseMap)
}


