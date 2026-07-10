package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentCommand
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get delete details`() = runTest {
        val id = fixture.createStudent(
            lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01",
        )

        val result = fixture.harness.query(DeleteStudentQuery(id))

        assertNotNull(result)
        assertEquals("Joe", result.firstMidName)
        assertEquals("Schmoe", result.lastName)
        assertEquals("2024-01-01", result.enrollmentDate)
    }

    @Test
    fun `should delete student`() = runTest {
        val id = fixture.createStudent(
            lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01",
        )

        fixture.harness.send(DeleteStudentCommand(id = id))

        assertNull(fixture.findStudent(id))
    }
}
