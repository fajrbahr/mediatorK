package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.detail.GetStudentQuery
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentCommand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EditTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get edit details`() = runTest {
        val id = fixture.createStudent(
            lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01",
        )

        val result = fixture.harness.query(GetStudentQuery(id))

        assertNotNull(result)
        assertEquals("Joe", result.firstMidName)
        assertEquals("Schmoe", result.lastName)
        assertEquals("2024-01-01", result.enrollmentDate)
    }

    @Test
    fun `should edit student`() = runTest {
        val id = fixture.createStudent(
            lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01",
        )

        val command = EditStudentCommand(
            id = id, lastName = "Smith", firstMidName = "Mary", enrollmentDate = "2023-01-01",
        )
        fixture.harness.send(command)

        val student = fixture.findStudent(id)
        assertNotNull(student)
        assertEquals(command.firstMidName, student.firstMidName)
        assertEquals(command.lastName, student.lastName)
        assertEquals(command.enrollmentDate, student.enrollmentDate)
    }
}
