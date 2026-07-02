package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.domain.CreateStudentCommand
import com.fajrbahr.mediatork.sample.university.student.domain.GetStudentQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create student`() = runTest {
        val id = fixture.harness.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )

        val student = fixture.harness.query(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Joe", student.firstMidName)
        assertEquals("Schmoe", student.lastName)
        assertEquals("2024-01-01", student.enrollmentDate)
    }

    @Test
    fun `should return positive id`() = runTest {
        val id = fixture.harness.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )

        assertTrue(id > 0)
    }

    @Test
    fun `should reject invalid data`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateStudentCommand(lastName = "", firstMidName = "", enrollmentDate = ""))
        }
    }
}
