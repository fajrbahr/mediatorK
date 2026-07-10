package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseCommand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EditTests {

    private val fixture = SliceFixture()

    @Test
    fun `should query for command`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val deptId = fixture.createDepartment(name = "History", administratorId = adminId)
        val id = fixture.createCourse(title = "English 101", credits = 4, departmentId = deptId)

        val result = fixture.harness.query(GetCourseQuery(id))

        assertNotNull(result)
        assertEquals("English 101", result.title)
        assertEquals(4, result.credits)
        assertEquals("History", result.departmentName)
    }

    @Test
    fun `should edit`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val deptId = fixture.createDepartment(name = "History", administratorId = adminId)
        val newDeptId = fixture.createDepartment(name = "English", administratorId = adminId)
        val id = fixture.createCourse(title = "English 101", credits = 4, departmentId = deptId)

        fixture.harness.send(
            EditCourseCommand(id = id, title = "English 202", credits = 5, departmentId = newDeptId)
        )

        val edited = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(edited)
        assertEquals("English", edited.departmentName)
        assertEquals(5, edited.credits)
        assertEquals("English 202", edited.title)
    }
}
