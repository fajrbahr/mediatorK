package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentCommand
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentQuery
import com.fajrbahr.mediatork.sample.university.student.detail.GetStudentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get delete details`() = runTest {
        val id = fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2025-01-15")

        val result = fixture.harness.query(DeleteStudentQuery(id))

        assertNotNull(result)
        assertEquals("Joe", result.firstMidName)
        assertEquals("Schmoe", result.lastName)
        assertEquals("2025-01-15", result.enrollmentDate)
    }

    @Test
    fun `should delete student`() = runTest {
        val id = fixture.createStudent()

        fixture.harness.send(DeleteStudentCommand(id = id))

        assertNull(fixture.harness.query(GetStudentQuery(id)))
    }
}
