package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.course.domain.EditCourseCommand
import com.fajrbahr.mediatork.sample.university.course.domain.EditCourseValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditCourseValidatorTest {

    private val validator = EditCourseValidator()

    @Test
    fun `valid edit passes`() {
        val result = validator.validate(EditCourseCommand(id = 1, title = "Chemistry", credits = 3))
        assertIs<ValidationResult.Valid>(result)
    }

    @Test
    fun `title too short is invalid`() {
        val result = validator.validate(EditCourseCommand(id = 1, title = "AB", credits = 3))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Title") })
    }

    @Test
    fun `credits out of range is invalid`() {
        val result = validator.validate(EditCourseCommand(id = 1, title = "Chemistry", credits = 10))
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.errors.any { it.toString().contains("Credits") })
    }
}
