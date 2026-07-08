package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateCourseValidatorTest {

    private val validator = CreateCourseValidator()

    @Test
    fun `valid command passes`() {
        val result = validator.validate(CreateCourseCommand(number = 1050, title = "Chemistry", credits = 3, departmentId = 1))
        assertIs<ValidationResult.Valid>(result)
    }

    @Test
    fun `number must be greater than 0`() {
        val result = validator.validate(CreateCourseCommand(number = 0, title = "Chemistry", credits = 3))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Number") })
    }

    @Test
    fun `title too short is invalid`() {
        val result = validator.validate(CreateCourseCommand(number = 1, title = "AB", credits = 3))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Title") })
    }

    @Test
    fun `title too long is invalid`() {
        val result = validator.validate(CreateCourseCommand(number = 1, title = "A".repeat(51), credits = 3))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Title") })
    }

    @Test
    fun `credits below 0 is invalid`() {
        val result = validator.validate(CreateCourseCommand(number = 1, title = "Chemistry", credits = -1))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Credits") })
    }

    @Test
    fun `credits above 5 is invalid`() {
        val result = validator.validate(CreateCourseCommand(number = 1, title = "Chemistry", credits = 6))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Credits") })
    }

    @Test
    fun `multiple errors are all collected`() {
        val result = validator.validate(CreateCourseCommand(number = 0, title = "", credits = -1))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.size >= 3)
    }
}
