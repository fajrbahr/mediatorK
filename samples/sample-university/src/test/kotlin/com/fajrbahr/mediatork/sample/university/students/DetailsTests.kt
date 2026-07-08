package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.detail.EnrollStudentCommand
import com.fajrbahr.mediatork.sample.university.student.detail.GetStudentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DetailsTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get student details`() = runTest {
        val id = fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe")

        val student = fixture.harness.query(GetStudentQuery(id))

        assertNotNull(student)
        assertEquals("Joe", student.firstMidName)
        assertEquals("Schmoe", student.lastName)
    }

    @Test
    fun `should include enrollments`() = runTest {
        val deptId = fixture.createDepartment(name = "English")
        val courseId1 = fixture.createCourse(title = "Course 1", credits = 4, departmentId = deptId)
        val courseId2 = fixture.createCourse(title = "Course 2", credits = 4, departmentId = deptId)
        val studentId = fixture.createStudent()

        fixture.harness.given(
            EnrollStudentCommand(studentId = studentId, courseId = courseId1, grade = Grade.A),
            EnrollStudentCommand(studentId = studentId, courseId = courseId2, grade = Grade.F),
        )

        val student = fixture.harness.query(GetStudentQuery(studentId))
        assertNotNull(student)
        assertEquals(2, student.enrollments.size)
    }
}
