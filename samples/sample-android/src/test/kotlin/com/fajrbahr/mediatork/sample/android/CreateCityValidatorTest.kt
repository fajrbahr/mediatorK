package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.sample.android.after.times.GetPrayerTimesRequest
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateCityValidatorTest {

    private fun valid(city: String, country: String = "") =
        GetPrayerTimesRequest(city = city, country = country).validate()

    private fun errorsOf(city: String, country: String = "") =
        (valid(city, country) as ValidationResult.Invalid).errors

    // ── Happy paths ───────────────────────────────────────────────────────────

    @Test
    fun `valid city passes`() {
        assertIs<ValidationResult.Valid>(valid("Dubai"))
    }

    @Test
    fun `city with spaces passes`() {
        assertIs<ValidationResult.Valid>(valid("New York"))
    }

    @Test
    fun `city with hyphen passes`() {
        assertIs<ValidationResult.Valid>(valid("Dar-es-Salaam"))
    }

    @Test
    fun `valid city with valid country passes`() {
        assertIs<ValidationResult.Valid>(valid("Makkah", "Saudi Arabia"))
    }

    @Test
    fun `empty country is allowed`() {
        assertIs<ValidationResult.Valid>(valid("Riyadh", ""))
    }

    // ── City validation failures ──────────────────────────────────────────────

    @Test
    fun `blank city is invalid`() {
        val result = valid("")
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it == "City must not be blank" })
    }

    @Test
    fun `city with one character is invalid`() {
        val result = valid("A")
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it == "City must be at least 2 characters" })
    }

    @Test
    fun `city with digits is invalid`() {
        val result = valid("Dubai1")
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it == "City must contain only letters, spaces, or hyphens" })
    }

    // ── Country validation failures ───────────────────────────────────────────

    @Test
    fun `country with one character is invalid`() {
        val result = valid("Dubai", "U")
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it == "Country must be at least 2 characters" })
    }

    @Test
    fun `country with digits is invalid`() {
        val result = valid("Dubai", "UAE1")
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it == "Country must contain only letters, spaces, or hyphens" })
    }

    @Test
    fun `multiple city errors are all collected`() {
        val errors = errorsOf("")
        assertTrue(errors.any { it == "City must not be blank" })
    }
}
