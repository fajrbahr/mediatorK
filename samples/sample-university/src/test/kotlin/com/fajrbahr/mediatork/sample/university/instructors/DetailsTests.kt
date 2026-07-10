package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.instructor.detail.GetInstructorQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DetailsTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get instructor details`() = runTest {
        val deptId = fixture.insertDepartment(name = "English")
        val courseId = fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)

        val id = fixture.createInstructor(
            lastName = "Costanza", firstMidName = "George",
            officeLocation = "Austin", selectedCourseIds = listOf(courseId),
        )

        val result = fixture.harness.query(GetInstructorQuery(id))

        assertNotNull(result)
        assertEquals("George", result.firstMidName)
        assertEquals("Austin", result.officeLocation)
    }
}
