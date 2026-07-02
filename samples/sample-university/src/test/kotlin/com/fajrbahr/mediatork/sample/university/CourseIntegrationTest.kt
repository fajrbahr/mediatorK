package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.course.domain.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.course.domain.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.course.domain.DeleteCourseCommand
import com.fajrbahr.mediatork.sample.university.course.domain.EditCourseCommand
import com.fajrbahr.mediatork.sample.university.course.domain.GetCourseQuery
import com.fajrbahr.mediatork.sample.university.course.domain.GetCoursesQuery
import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.test.buildHandlerTestHarness
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseIntegrationTest {

    private val harness = buildHandlerTestHarness(
        registrars = listOf(
            CourseRegistrar(CourseStore()),
            DepartmentRegistrar(DepartmentStore()),
        ),
    )

    private suspend fun createDept(name: String = "Engineering"): Int =
        harness.send(CreateDepartmentCommand(name = name, budget = 100.0, startDate = "2024-01-01"))

    // ── Create ──────────────────────────────────────────────────────────────────

    @Test
    fun `create course returns new id`() = runTest {
        val deptId = createDept()
        val id = harness.send(
            CreateCourseCommand(number = 1050, title = "Chemistry", credits = 3, departmentId = deptId)
        )
        assertTrue(id > 0)
    }

    @Test
    fun `create course with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            harness.send(CreateCourseCommand(number = 0, title = "", credits = -1))
        }
    }

    @Test
    fun `validation errors contain all failure messages`() = runTest {
        val ex = assertFailsWith<ValidationException> {
            harness.send(CreateCourseCommand(number = 0, title = "", credits = -1))
        }
        assertTrue(ex.errors.size >= 3)
    }

    @Test
    fun `created course is retrievable`() = runTest {
        val deptId = createDept("Mathematics")
        val id = harness.send(
            CreateCourseCommand(number = 2021, title = "Calculus", credits = 4, departmentId = deptId)
        )
        val course = harness.query(GetCourseQuery(id))
        assertNotNull(course)
        assertEquals("Calculus", course.title)
        assertEquals(4, course.credits)
        assertEquals(deptId, course.departmentId)
    }

    // ── Index ───────────────────────────────────────────────────────────────────

    @Test
    fun `list returns empty when no courses exist`() = runTest {
        val fresh = buildHandlerTestHarness(registrars = listOf(CourseRegistrar(CourseStore())))
        val courses = fresh.query(GetCoursesQuery)
        assertTrue(courses.isEmpty())
    }

    @Test
    fun `list returns all created courses`() = runTest {
        val engId = createDept("English")
        val mathId = createDept("Mathematics")
        harness.given(
            CreateCourseCommand(number = 101, title = "English Lit", credits = 3, departmentId = engId),
            CreateCourseCommand(number = 201, title = "Algebra", credits = 4, departmentId = mathId),
        )
        val courses = harness.query(GetCoursesQuery)
        assertEquals(2, courses.size)
    }

    @Test
    fun `list courses preserves department across different departments`() = runTest {
        val engId = createDept("English")
        val mathId = createDept("Mathematics")
        harness.given(
            CreateCourseCommand(number = 6001, title = "English 101", credits = 4, departmentId = engId),
            CreateCourseCommand(number = 6002, title = "History 101", credits = 4, departmentId = mathId),
        )
        val courses = harness.query(GetCoursesQuery)
        val deptIds = courses.map { it.departmentId }.toSet()
        assertTrue(deptIds.contains(engId))
        assertTrue(deptIds.contains(mathId))
    }

    // ── Details ─────────────────────────────────────────────────────────────────

    @Test
    fun `query returns course data for edit form`() = runTest {
        val deptId = createDept("English")
        val id = harness.send(
            CreateCourseCommand(number = 1060, title = "English 101", credits = 4, departmentId = deptId)
        )
        val course = harness.query(GetCourseQuery(id))
        assertNotNull(course)
        assertEquals(id, course.id)
        assertEquals("English 101", course.title)
        assertEquals(4, course.credits)
        assertEquals(deptId, course.departmentId)
    }

    // ── Edit ────────────────────────────────────────────────────────────────────

    @Test
    fun `edit course updates fields`() = runTest {
        val deptId = createDept("Engineering")
        val id = harness.send(
            CreateCourseCommand(number = 3030, title = "Physics I", credits = 3, departmentId = deptId)
        )
        harness.send(EditCourseCommand(id = id, title = "Physics II", credits = 4, departmentId = deptId))
        val updated = harness.query(GetCourseQuery(id))
        assertNotNull(updated)
        assertEquals("Physics II", updated.title)
        assertEquals(4, updated.credits)
    }

    @Test
    fun `edit course changes department`() = runTest {
        val engId = createDept("English")
        val econId = createDept("Economics")
        val id = harness.send(
            CreateCourseCommand(number = 3031, title = "Intro Econ", credits = 3, departmentId = engId)
        )
        harness.send(EditCourseCommand(id = id, title = "Intro Econ", credits = 3, departmentId = econId))
        val updated = harness.query(GetCourseQuery(id))
        assertNotNull(updated)
        assertEquals(econId, updated.departmentId)
    }

    @Test
    fun `edit with invalid data throws ValidationException`() = runTest {
        val deptId = createDept("Economics")
        val id = harness.send(
            CreateCourseCommand(number = 4040, title = "Economics", credits = 3, departmentId = deptId)
        )
        assertFailsWith<ValidationException> {
            harness.send(EditCourseCommand(id = id, title = "AB", credits = 10))
        }
    }

    @Test
    fun `edit non-existent course is a no-op`() = runTest {
        harness.send(EditCourseCommand(id = 9999, title = "Ghost", credits = 3))
        assertNull(harness.query(GetCourseQuery(9999)))
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @Test
    fun `query returns course data for delete confirmation`() = runTest {
        val deptId = createDept("English")
        val id = harness.send(
            CreateCourseCommand(number = 4041, title = "History 101", credits = 3, departmentId = deptId)
        )
        val course = harness.query(GetCourseQuery(id))
        assertNotNull(course)
        assertEquals("History 101", course.title)
        assertEquals(3, course.credits)
    }

    @Test
    fun `delete course removes it from store`() = runTest {
        val deptId = createDept("Economics")
        val id = harness.send(
            CreateCourseCommand(number = 5050, title = "Macro Econ", credits = 3, departmentId = deptId)
        )
        harness.send(DeleteCourseCommand(id))
        assertNull(harness.query(GetCourseQuery(id)))
    }

    @Test
    fun `delete non-existent course does not throw`() = runTest {
        harness.send(DeleteCourseCommand(9999))
    }
}
