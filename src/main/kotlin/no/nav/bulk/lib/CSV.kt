package no.nav.bulk.lib

import no.nav.bulk.generated.pdlquery.Vegadresse
import no.nav.bulk.models.MappedKRRPersonResponse
import no.nav.bulk.models.MappedKRRResponse
import no.nav.bulk.models.PDLResponse

private const val krrCsvHeader: String = "Personident,Språk,E-post,Mobilnummer,Adresse,Feil"
private const val krrAndPdlDataHeader: String =
    "Personident,Språk,E-post,Mobilnummer,Adresse,Fornavn,Mellomnavn,Etternavn,Dødsdato,Feil"

fun java.lang.StringBuilder.appendSpraakEpostPhoneFromKRRPersonData(
    personident: String,
    personData: MappedKRRPersonResponse
) {
    append(personident)
    append(',')
    append(personData.person?.spraak ?: "")
    append(',')
    append(personData.person?.epostadresse ?: "")
    append(',')
    append(personData.person?.mobiltelefonnummer ?: "")
}

private fun mapKrrDataToCSV(
    peopleData: MappedKRRResponse
): StringBuilder {
    val stringBuilder = StringBuilder()
    stringBuilder.append(krrCsvHeader)
    for ((personident, personData) in peopleData.personer) {
        stringBuilder.append("\n")
        stringBuilder.appendSpraakEpostPhoneFromKRRPersonData(personident, personData)
        stringBuilder.append(',')
        stringBuilder.append(personData.person?.adresse ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personData.feil?.value ?: "")
    }
    return stringBuilder
}

private fun implMapKrrAndPdlDataToCsv(
    krrData: MappedKRRResponse,
    pdlData: PDLResponse
): StringBuilder {
    val stringBuilder = StringBuilder()
    stringBuilder.append(krrAndPdlDataHeader)
    for ((personident, krrPerson) in krrData.personer) {
        val pdlPerson = pdlData[personident]
        stringBuilder.append("\n")
        stringBuilder.appendSpraakEpostPhoneFromKRRPersonData(personident, krrPerson)
        stringBuilder.append(',')
        stringBuilder.append(
            krrPerson.person?.adresse
                ?: pdlPerson
                    ?.bostedsadresse
                    ?.firstOrNull()
                    ?.vegadresse
                    ?.toAdresseString()
                ?: ""
        )
        stringBuilder.append(',')
        stringBuilder.append(pdlPerson?.navn?.firstOrNull()?.fornavn ?: "")
        stringBuilder.append(',')
        stringBuilder.append(pdlPerson?.navn?.firstOrNull()?.mellomnavn ?: "")
        stringBuilder.append(',')
        stringBuilder.append(pdlPerson?.navn?.firstOrNull()?.etternavn ?: "")
        stringBuilder.append(',')
        stringBuilder.append(pdlPerson?.doedsfall?.firstOrNull()?.doedsdato ?: "")
        stringBuilder.append(',')
        stringBuilder.append(krrPerson.feil?.value ?: "")
    }
    return stringBuilder
}

fun mapToCSV(krrData: MappedKRRResponse, pdlData: PDLResponse? = null): String {
    if (!pdlData.isNullOrEmpty())
        return implMapKrrAndPdlDataToCsv(krrData, pdlData).toString()
    return mapKrrDataToCSV(krrData).toString()
}

fun Vegadresse.toAdresseString(): String {
    return "${this.adressenavn ?: ""} ${this.husnummer ?: ""}${this.husbokstav ?: ""} ${this.postnummer ?: ""}"
}
