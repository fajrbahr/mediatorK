package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DetailsTests {

    private val fixture = SliceFixture()

    @Test
    fun `should query for details`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val deptId = fixture.insertDepartment(name = "History", administratorId = adminId)
        val courseId = fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)

        val result = fixture.harness.query(GetCourseQuery(courseId))

        assertNotNull(result)
        assertEquals(4, result.credits)
        assertEquals("History", result.departmentName)
        assertEquals("English 101", result.title)
    }
}
