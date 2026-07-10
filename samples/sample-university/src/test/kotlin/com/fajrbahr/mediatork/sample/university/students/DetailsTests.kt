package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.detail.GetStudentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DetailsTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get details`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val deptId = fixture.insertDepartment(name = "English 101", administratorId = adminId)
        val courseId1 = fixture.insertCourse(title = "Course 1", credits = 10, departmentId = deptId)
        val courseId2 = fixture.insertCourse(title = "Course 2", credits = 10, departmentId = deptId)

        val studentId = fixture.createStudent(
            lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2013-01-01",
        )

        fixture.insertEnrollment(courseId = courseId1, studentId = studentId, grade = Grade.A)
        fixture.insertEnrollment(courseId = courseId2, studentId = studentId, grade = Grade.F)

        val details = fixture.harness.query(GetStudentQuery(studentId))
        assertNotNull(details)
        assertEquals("Joe", details.firstMidName)
        assertEquals("Schmoe", details.lastName)
        assertEquals("2013-01-01", details.enrollmentDate)
        assertEquals(2, details.enrollments.size)
    }
}
