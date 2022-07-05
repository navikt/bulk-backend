package no.nav.bulk

import no.nav.bulk.models.*
import java.time.LocalDate

object FilterTestData {

    val INPUT_KAN_VARSLES_TRUE = DigDirResponse(
        personer = mapOf(
            "1234" to DigDirPersonInfo(
                personident = "1234",
                aktiv = true,
                kanVarsles = true,
                reservert = false,
                spraak = "nb",
                epostadresse = "ola@nordmann.no",
                epostadresseOppdatert = LocalDate.now().toString(),
                mobiltelefonnummer = "12345678",
                mobiltelefonnummerOppdatert = LocalDate.now().toString(),
                sikkerDigitalPostkasse = SikkerDigitalPostkasse(
                    adresse = "Nordmannveien 12A",
                    leverandoerAdresse = "Digipost",
                    leverandoerSertifikat = "noe"
                )
            )
        ),
        feil = emptyMap()
    )
    val RESULT_KAN_VALRSLES_TRUE = PersonInfoResponse(
        personer = mapOf(
            "1234" to PersonData(
                person = Person(
                    personident = "1234",
                    spraak = "nb",
                    epostadresse = "ola@nordmann.no",
                    mobiltelefonnummer = "12345678",
                    addresse = "Nordmannveien 12A"
                ), feil = null
            )
        )
    )
}