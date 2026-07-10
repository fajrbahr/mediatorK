package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentCommand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create student`() = runTest {
        val cmd = CreateStudentCommand(
            lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01",
        )
        val studentId = fixture.harness.send(cmd)

        val student = fixture.findStudent(studentId)
        assertNotNull(student)
        assertEquals(cmd.firstMidName, student.firstMidName)
        assertEquals(cmd.lastName, student.lastName)
        assertEquals(cmd.enrollmentDate, student.enrollmentDate)
    }
}
