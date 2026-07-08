package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.list.GetCoursesQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should return empty when no courses exist`() = runTest {
        val fresh = SliceFixture()

        val result = fresh.harness.query(GetCoursesQuery)

        assertTrue(result.courses.isEmpty())
    }

    @Test
    fun `should return all courses`() = runTest {
        val engId = fixture.createDepartment(name = "English")
        val mathId = fixture.createDepartment(name = "Mathematics")
        fixture.createCourse(title = "English Lit", credits = 3, departmentId = engId)
        fixture.createCourse(title = "Algebra", credits = 4, departmentId = mathId)

        val result = fixture.harness.query(GetCoursesQuery)

        assertEquals(2, result.courses.size)
    }

    @Test
    fun `should preserve department across courses`() = runTest {
        val engId = fixture.createDepartment(name = "English")
        val mathId = fixture.createDepartment(name = "Mathematics")
        fixture.createCourse(title = "English 101", credits = 4, departmentId = engId)
        fixture.createCourse(title = "History 101", credits = 4, departmentId = mathId)

        val result = fixture.harness.query(GetCoursesQuery)
        val deptNames = result.courses.map { it.departmentName }.toSet()

        assertTrue(deptNames.contains("English"))
        assertTrue(deptNames.contains("Mathematics"))
    }
}
