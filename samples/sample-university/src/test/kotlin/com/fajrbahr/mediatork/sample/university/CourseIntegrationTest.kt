package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.course.detail.DeleteCourseCommand
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseCommand
import com.fajrbahr.mediatork.sample.university.course.list.GetCoursesQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseIntegrationTest {

    private val fixture = SliceFixture()

    @Test
    fun `create course returns new id`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.harness.send(
            CreateCourseCommand(number = 1050, title = "Chemistry", credits = 3, departmentId = deptId)
        )
        assertTrue(id > 0)
    }

    @Test
    fun `create course with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateCourseCommand(number = 0, title = "", credits = -1))
        }
    }

    @Test
    fun `validation errors contain all failure messages`() = runTest {
        val ex = assertFailsWith<ValidationException> {
            fixture.harness.send(CreateCourseCommand(number = 0, title = "", credits = -1))
        }
        assertTrue(ex.errors.size >= 3)
    }

    @Test
    fun `created course is retrievable`() = runTest {
        val deptId = fixture.createDepartment(name = "Mathematics")
        val id = fixture.harness.send(
            CreateCourseCommand(number = 2021, title = "Calculus", credits = 4, departmentId = deptId)
        )
        val course = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(course)
        assertEquals("Calculus", course.title)
        assertEquals(4, course.credits)
        assertEquals("Mathematics", course.departmentName)
    }

    @Test
    fun `list returns empty when no courses exist`() = runTest {
        val fresh = SliceFixture()
        val result = fresh.harness.query(GetCoursesQuery)
        assertTrue(result.courses.isEmpty())
    }

    @Test
    fun `list returns all created courses`() = runTest {
        val engId = fixture.createDepartment(name = "English")
        val mathId = fixture.createDepartment(name = "Mathematics")
        fixture.harness.send(CreateCourseCommand(number = 101, title = "English Lit", credits = 3, departmentId = engId))
        fixture.harness.send(CreateCourseCommand(number = 201, title = "Algebra", credits = 4, departmentId = mathId))
        val result = fixture.harness.query(GetCoursesQuery)
        assertEquals(2, result.courses.size)
    }

    @Test
    fun `list courses preserves department across different departments`() = runTest {
        val engId = fixture.createDepartment(name = "English")
        val mathId = fixture.createDepartment(name = "Mathematics")
        fixture.harness.send(CreateCourseCommand(number = 6001, title = "English 101", credits = 4, departmentId = engId))
        fixture.harness.send(CreateCourseCommand(number = 6002, title = "History 101", credits = 4, departmentId = mathId))
        val result = fixture.harness.query(GetCoursesQuery)
        val deptNames = result.courses.map { it.departmentName }.toSet()
        assertTrue(deptNames.contains("English"))
        assertTrue(deptNames.contains("Mathematics"))
    }

    @Test
    fun `edit course updates fields`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.harness.send(
            CreateCourseCommand(number = 3030, title = "Physics I", credits = 3, departmentId = deptId)
        )
        fixture.harness.send(EditCourseCommand(id = id, title = "Physics II", credits = 4, departmentId = deptId))
        val updated = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(updated)
        assertEquals("Physics II", updated.title)
        assertEquals(4, updated.credits)
    }

    @Test
    fun `edit course changes department`() = runTest {
        val engId = fixture.createDepartment(name = "English")
        val econId = fixture.createDepartment(name = "Economics")
        val id = fixture.harness.send(
            CreateCourseCommand(number = 3031, title = "Intro Econ", credits = 3, departmentId = engId)
        )
        fixture.harness.send(EditCourseCommand(id = id, title = "Intro Econ", credits = 3, departmentId = econId))
        val updated = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(updated)
        assertEquals("Economics", updated.departmentName)
    }

    @Test
    fun `edit with invalid data throws ValidationException`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.harness.send(
            CreateCourseCommand(number = 4040, title = "Economics", credits = 3, departmentId = deptId)
        )
        assertFailsWith<ValidationException> {
            fixture.harness.send(EditCourseCommand(id = id, title = "AB", credits = 10))
        }
    }

    @Test
    fun `edit non-existent course is a no-op`() = runTest {
        fixture.harness.send(EditCourseCommand(id = 9999, title = "Ghost", credits = 3))
        assertNull(fixture.harness.query(GetCourseQuery(9999)))
    }

    @Test
    fun `delete course removes it from store`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.harness.send(
            CreateCourseCommand(number = 5050, title = "Macro Econ", credits = 3, departmentId = deptId)
        )
        fixture.harness.send(DeleteCourseCommand(id))
        assertNull(fixture.harness.query(GetCourseQuery(id)))
    }

    @Test
    fun `delete non-existent course does not throw`() = runTest {
        fixture.harness.send(DeleteCourseCommand(9999))
    }
}
