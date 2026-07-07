package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.course.domain.CreateCourseCommand
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateCourseValidatorTest {

    @Test
    fun `valid command passes`() {
        val result = CreateCourseCommand(number = 1050, title = "Chemistry", credits = 3, departmentId = 1).validate()
        assertIs<ValidationResult.Valid>(result)
    }

    @Test
    fun `number must be greater than 0`() {
        val result = CreateCourseCommand(number = 0, title = "Chemistry", credits = 3).validate()
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Number") })
    }

    @Test
    fun `title too short is invalid`() {
        val result = CreateCourseCommand(number = 1, title = "AB", credits = 3).validate()
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Title") })
    }

    @Test
    fun `title too long is invalid`() {
        val result = CreateCourseCommand(number = 1, title = "A".repeat(51), credits = 3).validate()
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Title") })
    }

    @Test
    fun `credits below 0 is invalid`() {
        val result = CreateCourseCommand(number = 1, title = "Chemistry", credits = -1).validate()
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Credits") })
    }

    @Test
    fun `credits above 5 is invalid`() {
        val result = CreateCourseCommand(number = 1, title = "Chemistry", credits = 6).validate()
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Credits") })
    }

    @Test
    fun `multiple errors are all collected`() {
        val result = CreateCourseCommand(number = 0, title = "", credits = -1).validate()
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.size >= 3)
    }
}
