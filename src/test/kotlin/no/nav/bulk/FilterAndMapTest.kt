package no.nav.bulk

import no.nav.bulk.lib.filterAndMapDigDirResponse
import no.nav.bulk.models.DigDirPersonInfo
import no.nav.bulk.models.DigDirResponse
import no.nav.bulk.models.PersonInfoResponse
import org.junit.jupiter.api.Test
import java.util.logging.Filter
import kotlin.test.assertEquals

class FilterAndMapTest {

    /**
     * Test cases:
     * 1. Test that for an input of a person where kanVarsles is true, a corresponding PersonInfoResponse is returned.
     * 2. Test that for an input of a person where kanVarsles is false or reservert is false or aktiv is false,
     * FeilType "reservert" is returned
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
        val expected = FilterTestData.RESULT_KAN_VALRSLES_TRUE
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_KAN_VARSLES_TRUE)
        assertEquals(expected, actual, "The correct PersonInfoResponse object was not returned for kanVarsles is true.")
    }

    @Test
    fun testKanVarslesIsFalse() {
        val expected = FilterTestData.RESULT_KAN_VALRSLES_FALSE
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_KAN_VARSLES_FALSE)
        assertEquals(
            expected,
            actual,
            "When kanVarsles is false, the PersonInfoResponse.feil should be reserved and person should be null."
        )
    }

    @Test
    fun testOutdatedContactInfo() {
        val expected = FilterTestData.RESULT_OUTDATED_CONTACT_INFO
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_OUTDATED_CONTACT_INFO)
        assertEquals(
            expected, actual, "If both mobiltelefonnummerOppdatert and " +
                    "epostaddresseOppdatert are older than 18 months, the result should have UTDATERT_KONTAKTINFORMASJON."
        )
    }

    @Test
    fun testOutdatedEpostNotNumber() {
        val expected = FilterTestData.RESULT_OUTDATED_EPOST_NOT_NUMBER
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_OUTDATED_EPOST_NOT_NUMBER)
        assertEquals(
            expected,
            actual,
            "If only the epoostadresseOppdatert is older than 18 months, " +
                    "the result should have epostadresse set to null."
        )
    }

    @Test
    fun testOutdatedNumberNotEpost() {
        val expected = FilterTestData.RESULT_OUTDATED_NUMBER_NOT_EPOST
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_OUTDATED_NUMBER_NOT_EPOST)
        assertEquals(
            expected,
            actual,
            "If only mobiltelefonnummerOppdatert is older than 18 months, " +
                    "the result should have mobiltelefonnummer set to null."
        )
    }

    @Test
    fun testOutdatedEpostNumberNull() {
        val expected = FilterTestData.RESULT_OUTDATED_EPOST_NUMBER_NULL
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_OUTDATED_EPOST_NUMBER_NULL)
        assertEquals(
            expected,
            actual,
            "If the epostadresseOppdatert is older than 18 months, " +
                    "but the mobiltelefonnummer is null, return UTDATERT_KONTAKTINFORMASJON"
        )
    }

    @Test
    fun testOutdatedNumberEpostNull() {
        val expected = FilterTestData.RESULT_OUTDATED_NUMBER_EPOST_NULL
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_OUTDATED_EPOST_NUMBER_NULL)
        assertEquals(
            expected,
            actual,
            "If the mobiltelefonnummerOppdatert is older than 18 months, " +
                    "but the epostadresse is null, return UTDATERT_KONTAKTINFORMASJON"
        )
    }
    @Test
    fun testFeilPersonIkkeFunnet() {
        val expected = FilterTestData.RESULT_FEIL_PERSON_IKKE_FUNNET
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_FEIL_PERSON_IKKE_FUNNET)
        assertEquals(
            expected,
            actual,
            "If there is an error on person being PERSON_IKKE_FUNNET, " +
                    "this should also be relfected in the result."
        )
    }

    @Test
    fun testFeilStrengtFortroligAdresse() {
        val expected = FilterTestData.RESULT_FEIL_STRENGT_FORTROLIG_ADRESSE
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_FEIL_STRENGT_FORTROLIG_ADRESSE)
        assertEquals(
            expected,
            actual,
            "If there is an error on person being STRENGT_FORTROLIG_ADRESSE, " +
                    "this should also be relfected in the result."
        )
    }


    @Test
    fun testFeilStrengtFortroligUtenlandskAdresse() {
        val expected = FilterTestData.RESULT_FEIL_STRENGT_FORTROLIG_UTENLANDSK_ADRESSE
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_FEIL_STRENGT_FORTROLIG_UTENLANDSK_ADRESSE)
        assertEquals(
            expected,
            actual,
            "If there is an error on person being STRENGT_FORTROLIG_UTENLANDSK_ADRESSE, " +
                    "this should also be relfected in the result."
        )
    }

    @Test
    fun testFeilFortroligAdresse() {
        val expected = FilterTestData.RESULT_FEIL_FORTROLIG_ADRESSE
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_FEIL_FORTROLIG_ADRESSE)
        assertEquals(
            expected,
            actual,
            "If there is an error on person being FORTROLIG_ADRESSE, " +
                    "this should also be relfected in the result."
        )
    }

    @Test
    fun testFeilSkjermet() {
        val expected = FilterTestData.RESULT_FEIL_SKJERMET
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_FEIL_SKJERMET)
        assertEquals(
            expected,
            actual,
            "If there is an error on person being SKJERMET, " +
                    "this should also be relfected in the result."
        )
    }

    @Test
    fun testMultiplePeople() {
        val expected = FilterTestData.RESULT_MULTIPLE_PEOPLE
        val actual = filterAndMapDigDirResponse(FilterTestData.INPUT_MULTIPLE_PEOPLE)
        assertEquals(
            expected,
            actual,
            "Test failed when taking multiple people as input. Seems like the combined function " +
                    "or list support of the tested function is not functioning as expected."
        )


    }
}