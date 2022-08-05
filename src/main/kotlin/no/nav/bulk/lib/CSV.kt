package no.nav.bulk.lib

import no.nav.bulk.generated.pdlquery.Person
import no.nav.bulk.generated.pdlquery.Vegadresse
import no.nav.bulk.models.PDLResponse
import no.nav.bulk.models.PeopleDataResponse
import no.nav.bulk.models.PersonData

private const val krrCsvHeader: String = "Personident,Språk,E-post,Mobilnummer,Adresse,Feil"
private const val krrAndPdlDataHeader: String =
    "Personident,Språk,E-post,Mobilnummer,Adresse,Fornavn,Mellomnavn,Etternavn,Feil"

private fun mapKrrDataToCSV(
    peopleData: PeopleDataResponse
): StringBuilder {
    val stringBuilder = StringBuilder()
    stringBuilder.append(krrCsvHeader)
    for ((personident, personData) in peopleData.personer) {
        stringBuilder.append("\n")
        stringBuilder.append(personident)
        stringBuilder.append(',')
        stringBuilder.append(personData.person?.spraak ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personData.person?.epostadresse ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personData.person?.mobiltelefonnummer ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personData.person?.adresse ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personData.feil?.value ?: "")
    }
    return stringBuilder
}

private fun implMapKrrAndPdlDataToCsv(
    krrData: PeopleDataResponse,
    pdlData: PDLResponse
): StringBuilder {
    val stringBuilder = StringBuilder()
    stringBuilder.append(krrAndPdlDataHeader)
    for ((personident, personDataPair) in mergeKrrAndPdlData(krrData, pdlData)) {
        stringBuilder.append("\n")
        stringBuilder.append(personident)
        stringBuilder.append(',')
        stringBuilder.append(personDataPair.first.person?.spraak ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personDataPair.first.person?.epostadresse ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personDataPair.first.person?.mobiltelefonnummer ?: "")
        stringBuilder.append(',')
        stringBuilder.append(
            personDataPair.first.person?.adresse
                ?: personDataPair
                    .second
                    ?.bostedsadresse
                    ?.firstOrNull()
                    ?.vegadresse
                    ?.toAdresseString()
                ?: ""
        )
        stringBuilder.append(',')
        stringBuilder.append(personDataPair.second?.navn?.firstOrNull()?.fornavn ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personDataPair.second?.navn?.firstOrNull()?.mellomnavn ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personDataPair.second?.navn?.firstOrNull()?.etternavn ?: "")
        stringBuilder.append(',')
        stringBuilder.append(personDataPair.first.feil?.value ?: "")
    }
    return stringBuilder
}

fun mapToCSV(krrData: PeopleDataResponse, pdlData: PDLResponse? = null): String {
    val krrDataIsValid = krrData.personer.isNotEmpty()
    if (krrDataIsValid && !pdlData.isNullOrEmpty()) {
        return implMapKrrAndPdlDataToCsv(krrData, pdlData).toString()
    } else if (krrDataIsValid) {
        return mapKrrDataToCSV(krrData).toString()
    }
    return ""
}

fun Vegadresse.toAdresseString(): String {
    return "${this.adressenavn ?: ""} ${this.husnummer ?: ""}${this.husbokstav ?: ""} ${this.postnummer ?: ""}"
}

fun mergeKrrAndPdlData(
    krrData: PeopleDataResponse,
    pdlData: PDLResponse
): Map<String, Pair<PersonData, Person?>> {
    val unionList: MutableMap<String, PersonData> = krrData.personer.toMutableMap()
    return unionList.mapValues { (personident, personData) ->
        Pair(personData, pdlData.getValue(personident))
    }
}
