package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.list.GetCoursesQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should return all courses`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Jones", firstMidName = "George")
        val engId = fixture.insertDepartment(name = "English", administratorId = adminId)
        val histId = fixture.insertDepartment(name = "History", administratorId = adminId)
        val engCourseId = fixture.insertCourse(title = "English 101", credits = 4, departmentId = engId)
        val histCourseId = fixture.insertCourse(title = "History 101", credits = 4, departmentId = histId)

        val result = fixture.harness.query(GetCoursesQuery)

        assertTrue(result.courses.size >= 2)
        assertTrue(result.courses.any { it.id == engCourseId })
        assertTrue(result.courses.any { it.id == histCourseId })
    }
}
