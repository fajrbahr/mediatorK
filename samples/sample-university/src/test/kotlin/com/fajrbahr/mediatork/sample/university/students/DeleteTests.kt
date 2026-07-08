package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.detail.DeleteStudentCommand
import com.fajrbahr.mediatork.sample.university.student.detail.GetStudentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get delete details`() = runTest {
        val id = fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe")

        val student = fixture.harness.query(GetStudentQuery(id))

        assertNotNull(student)
    }

    @Test
    fun `should delete student`() = runTest {
        val id = fixture.createStudent()

        fixture.harness.send(DeleteStudentCommand(id))

        assertNull(fixture.harness.query(GetStudentQuery(id)))
    }
}
