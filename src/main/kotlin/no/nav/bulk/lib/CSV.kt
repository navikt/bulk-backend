package no.nav.bulk.lib

import no.nav.bulk.models.PeopleDataResponse

fun mapToCSV(peopleData: PeopleDataResponse): String {
    val p = StringBuilder()
    p.append("Personident,Språk,E-post,Mobilnummer,Adresse,Feil")
    for ((personident, personData) in peopleData.personer) {
        p.append("\n")
        p.append(personident)
        p.append(',')
        p.append(personData.person?.spraak ?: "")
        p.append(',')
        p.append(personData.person?.epostadresse ?: "")
        p.append(',')
        p.append(personData.person?.mobiltelefonnummer ?: "")
        p.append(',')
        p.append(personData.person?.adresse ?: "")
        p.append(',')
        p.append(personData.feil ?: "")
    }
    return p.toString()
}
