package no.nav.bulk.lib

import no.nav.bulk.models.PeopleDataResponse

fun mapToCSV(peopleData: PeopleDataResponse): String {
    return peopleData.personer.map { (personident, personData) ->
        val p = StringBuilder()
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
        p.toString()
    }.joinToString(separator = "\n").also {
        it.replace("/^/","Personident,Språk,E-post,Mobilnummer,Adresse,Feil\n")
    }
}