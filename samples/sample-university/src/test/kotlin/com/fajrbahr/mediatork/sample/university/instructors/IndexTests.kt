package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.instructor.list.GetInstructorsQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get list of instructors with details`() = runTest {
        val deptId = fixture.insertDepartment(name = "English")
        val courseId1 = fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)
        val courseId2 = fixture.insertCourse(title = "English 201", credits = 4, departmentId = deptId)

        val instructor1Id = fixture.createInstructor(
            lastName = "Costanza", firstMidName = "George",
            officeLocation = "Austin", selectedCourseIds = listOf(courseId1, courseId2),
        )
        val instructor2Id = fixture.createInstructor(
            lastName = "Seinfeld", firstMidName = "Jerry",
            officeLocation = "Houston",
        )

        val student1Id = fixture.insertStudent(lastName = "Kramer", firstMidName = "Cosmo")
        val student2Id = fixture.insertStudent(lastName = "Benes", firstMidName = "Elaine")

        fixture.insertEnrollment(courseId = courseId1, studentId = student1Id)
        fixture.insertEnrollment(courseId = courseId1, studentId = student2Id)

        val result = fixture.harness.query(
            GetInstructorsQuery(
                selectedInstructorId = instructor1Id,
                selectedCourseId = courseId1,
            ),
        )

        assertTrue(result.instructors.size >= 2)
        assertTrue(result.instructors.any { it.id == instructor1Id })
        assertTrue(result.instructors.any { it.id == instructor2Id })
        assertEquals(2, result.courses.size)
        assertEquals(2, result.enrollments.size)
    }
}
