package no.nav.bulk.lib

import no.nav.bulk.models.*
import java.time.LocalDateTime


fun parseDate(date: String?): LocalDateTime? {
    return if (date != null) LocalDateTime.parse(date) else null
}

fun isValidDate(date: LocalDateTime?): Boolean {
    val now = LocalDateTime.now()
    return if (date != null) now.minusMonths(18).isBefore(date) else false
}
fun filterAndMapDigDirResponse(digDirResponse: DigDirResponse): PersonInfoResponse {
    val personResponseMap = mutableMapOf<String, PersonData>()

    for ((personident, personInfo) in digDirResponse.personer) {
        val lastUpdatedEmail = parseDate(personInfo.epostadresseOppdatert)
        val lastUpdatedPhone = parseDate(personInfo.mobiltelefonnummerOppdatert)
        // email is out of date
        if (!isValidDate(lastUpdatedEmail) && !isValidDate(lastUpdatedPhone)) {
            personResponseMap[personident] = PersonData(null, FeilType.UTDATERT_KONTAKTINFORMASJON)
            continue
        }
        // tlf is out of date

        // When person is "null" in DigDir, it means that the person is not allowed to be contacted
        if (personInfo.kanVarsles == null || personInfo.kanVarsles == false) {
            personResponseMap[personident] = PersonData(null, FeilType.RESERVERT)
            continue
        }

        // test outdated contact info



    }
    return PersonInfoResponse(personResponseMap)
}