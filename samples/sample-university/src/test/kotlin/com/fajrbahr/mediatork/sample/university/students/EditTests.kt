package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.domain.EditStudentCommand
import com.fajrbahr.mediatork.sample.university.student.domain.GetStudentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EditTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get edit details`() = runTest {
        val id = fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe")

        val student = fixture.harness.query(GetStudentQuery(id))

        assertNotNull(student)
        assertEquals("Joe", student.firstMidName)
        assertEquals("Schmoe", student.lastName)
        assertEquals("2024-01-01", student.enrollmentDate)
    }

    @Test
    fun `should edit student`() = runTest {
        val id = fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe")

        fixture.harness.send(
            EditStudentCommand(id = id, lastName = "Smith", firstMidName = "Mary", enrollmentDate = "2023-01-01")
        )

        val student = fixture.harness.query(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Mary", student.firstMidName)
        assertEquals("Smith", student.lastName)
        assertEquals("2023-01-01", student.enrollmentDate)
    }
}
