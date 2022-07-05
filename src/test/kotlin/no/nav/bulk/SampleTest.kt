package no.nav.bulk

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SampleTest {

    @BeforeAll
    fun beforeAll() {
        println("Before all")
    }

    @BeforeEach
    fun setUp() {
        println("Setup")
    }

    @AfterEach
    fun tearDown() {
        println("Tear down")
    }

    @AfterAll
    fun afterAll() {
        println("After all")
    }

    @Test
    fun sum() {
        assertEquals(4, Sample().sum(2, 2))
    }
}