import no.nav.bulk.models.PeopleDataResponse

fun mapToCSV(peopleData: PeopleDataResponse): String {
    var result = "Personident,Språk,E-post,Mobilnummer,Adresse,Feil"
    peopleData.personer.forEach { (personident, personData) ->
        result += "\n$personident," +
                (personData.person?.spraak ?: "") + "," +
                (personData.person?.epostadresse ?: "") + "," +
                (personData.person?.mobiltelefonnummer ?: "") + "," +
                (personData.person?.adresse ?: "") + "," +
                (personData.feil ?: "")
    }
    return result
}