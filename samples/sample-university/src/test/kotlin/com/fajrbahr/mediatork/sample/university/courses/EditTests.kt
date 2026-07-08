package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseCommand
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EditTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get edit details`() = runTest {
        val deptId = fixture.createDepartment(name = "English")
        val id = fixture.createCourse(title = "English 101", credits = 4, departmentId = deptId)

        val result = fixture.harness.query(GetCourseQuery(id))

        assertNotNull(result)
        assertEquals("English 101", result.title)
        assertEquals(4, result.credits)
    }

    @Test
    fun `should edit course`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.createCourse(title = "Physics I", credits = 3, departmentId = deptId)

        fixture.harness.send(EditCourseCommand(id = id, title = "Physics II", credits = 4, departmentId = deptId))

        val updated = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(updated)
        assertEquals("Physics II", updated.title)
        assertEquals(4, updated.credits)
    }

    @Test
    fun `should change department`() = runTest {
        val engId = fixture.createDepartment(name = "English")
        val econId = fixture.createDepartment(name = "Economics")
        val id = fixture.createCourse(title = "Intro Econ", credits = 3, departmentId = engId)

        fixture.harness.send(EditCourseCommand(id = id, title = "Intro Econ", credits = 3, departmentId = econId))

        val updated = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(updated)
        assertEquals("Economics", updated.departmentName)
    }

    @Test
    fun `should reject invalid edit`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.createCourse(title = "Economics", credits = 3, departmentId = deptId)

        assertFailsWith<ValidationException> {
            fixture.harness.send(EditCourseCommand(id = id, title = "AB", credits = 10))
        }
    }

    @Test
    fun `should no-op for non-existent course`() = runTest {
        fixture.harness.send(EditCourseCommand(id = 9999, title = "Ghost", credits = 3))

        assertNull(fixture.harness.query(GetCourseQuery(9999)))
    }
}
