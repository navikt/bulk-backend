package no.nav.bulk

import no.nav.bulk.lib.filterAndMapDigDirResponse
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.PersonInfoResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FilterAndMapTest  {

    /**
     * Test cases:
     * 1. Test that for an input of a person where kanVarsles is true, a corresponding PersonInfoResponse is returned.
     * 2. Test that for an input of a person where kanVarsles is false or reservert is false, FeilType "reservert" is returned
     * 3. Test that for an input where both epostadresseOppdatert
     * and mobiltelefonnummerOppdatert is older than 18 months, FeilType utdatert_kontaktinformasjon is returned.
     * 4. Test that for an input where epostadresseOppdaert is older than 18 months, but mobiltelefonnummerOppdatert is
     * not older than 18 months, a success response is returned only containing the phone number.
     * 5. Test that for an input where epostadresseOppdaert is not older than 18 months, but mobiltelefonnummerOppdatert is
     * older than 18 months, a success response is returned only containing the epost.
     * 6. Test that for an input where there is "feil" on the input person "person_ikke_funnet", the equivalent FeiType
     * is returned.
     * 7. Test that for an input where there is "feil" on the input person "strengt_fortrolig_adresse", the equivalent FeilType
     * is returned.
     * 8. Test that for an input where there is "feil" on the input person "strengt_fortrolig_utenlandsk_adresse", the equivalent FeilType
     * is returned.
     * 9. Test that for an input where there is "feil" on the input person "fortrolig_adresse", the equivalent FeilType
     * is returned.
     * 10. Test that for an input where there is "feil" on the input person "skjermet", the equivalent FeilType
     * is returned.
     * 11. Test for an input of multiple people with different parameters, the correct data is returned.
     */


    @Test
    fun testKanVarslesIsTrue() {
        val testData = DigDirResponse(mapOf(), mapOf())
        val expected = PersonInfoResponse(mapOf())
        val actual = filterAndMapDigDirResponse(testData)
        assertEquals(expected, actual, "The correct PersonInfoResponse object was not returned for kanVarsles is true.")
    }

}