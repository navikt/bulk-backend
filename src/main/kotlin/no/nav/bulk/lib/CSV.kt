package no.nav.bulk.lib

import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataResponse

fun mapToCSV(peopleData: PeopleDataResponse): String {
    // With 200k pnrs, this processing can take 7 minutes!!!
    return peopleData.personer.map { (personident, personData) ->
        var p = StringBuilder()
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