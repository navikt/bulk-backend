import no.nav.bulk.logger
import no.nav.bulk.models.PeopleDataResponse
import java.time.LocalDateTime

fun mapToCSV(peopleData: PeopleDataResponse): String {
    var result = "Personident,Språk,E-post,Mobilnummer,Adresse,Feil"
    logger.info("Mapping people request to CSV file (string)")
    logger.info("Time start mapping to CSV: ${LocalDateTime.now()}")
    // With 200k pnrs, this processing can take 7 minutes!!!
    peopleData.personer.forEach { (personident, personData) ->
        result += "\n$personident," +
                (personData.person?.spraak ?: "") + "," +
                (personData.person?.epostadresse ?: "") + "," +
                (personData.person?.mobiltelefonnummer ?: "") + "," +
                (personData.person?.adresse ?: "") + "," +
                (personData.feil ?: "")
    }
    logger.info("Time end mapping to CSV: ${LocalDateTime.now()}")
    return result
}