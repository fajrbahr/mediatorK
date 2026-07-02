package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.course.domain.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.course.domain.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.domain.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.instructor.domain.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.domain.DeleteInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.domain.GetInstructorQuery
import com.fajrbahr.mediatork.sample.university.instructor.domain.GetInstructorsQuery
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorStore
import com.fajrbahr.mediatork.test.buildHandlerTestHarness
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstructorIntegrationTest {

    private val deptStore = DepartmentStore()
    private val harness = buildHandlerTestHarness(
        registrars = listOf(
            InstructorRegistrar(InstructorStore(), deptStore),
            DepartmentRegistrar(deptStore),
            CourseRegistrar(CourseStore()),
        ),
    )

    private suspend fun createDept(name: String = "English"): Int =
        harness.send(CreateDepartmentCommand(name = name, budget = 100.0, startDate = "2024-01-01"))

    // ── CreateEdit ──────────────────────────────────────────────────────────────

    @Test
    fun `create instructor returns new id`() = runTest {
        val deptId = createDept()
        val courseId1 =
            harness.send(CreateCourseCommand(number = 101, title = "English 101", credits = 4, departmentId = deptId))
        val courseId2 =
            harness.send(CreateCourseCommand(number = 201, title = "English 201", credits = 4, departmentId = deptId))

        val id = harness.send(
            CreateEditInstructorCommand(
                lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = listOf(courseId1, courseId2),
            )
        )
        val created = harness.query(GetInstructorQuery(id))
        assertNotNull(created)
        assertEquals("Jerry", created.firstMidName)
        assertEquals("Seinfeld", created.lastName)
        assertEquals("Houston", created.officeLocation)
        assertEquals(2, created.courseIds.size)
    }

    @Test
    fun `create instructor with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            harness.send(CreateEditInstructorCommand(lastName = "", firstMidName = "", hireDate = ""))
        }
    }

    // ── Edit ────────────────────────────────────────────────────────────────────

    @Test
    fun `edit instructor updates fields`() = runTest {
        val id = harness.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin",
            )
        )
        harness.send(
            CreateEditInstructorCommand(
                id = id, lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = emptyList(),
            )
        )
        val edited = harness.query(GetInstructorQuery(id))
        assertNotNull(edited)
        assertEquals("Jerry", edited.firstMidName)
        assertEquals("Seinfeld", edited.lastName)
        assertEquals("Houston", edited.officeLocation)
    }

    // ── Merge courses ───────────────────────────────────────────────────────────

    @Test
    fun `edit instructor merges course assignments`() = runTest {
        val deptId = createDept()
        val courseId1 =
            harness.send(CreateCourseCommand(number = 301, title = "English 101", credits = 4, departmentId = deptId))
        val courseId2 =
            harness.send(CreateCourseCommand(number = 302, title = "English 201", credits = 4, departmentId = deptId))

        val id = harness.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin", selectedCourseIds = listOf(courseId1),
            )
        )
        harness.send(
            CreateEditInstructorCommand(
                id = id, lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = listOf(courseId2),
            )
        )
        val edited = harness.query(GetInstructorQuery(id))
        assertNotNull(edited)
        assertEquals(1, edited.courseIds.size)
        assertEquals(courseId2, edited.courseIds.first())
    }

    // ── Details ─────────────────────────────────────────────────────────────────

    @Test
    fun `query returns instructor details`() = runTest {
        val id = harness.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin",
            )
        )
        val instructor = harness.query(GetInstructorQuery(id))
        assertNotNull(instructor)
        assertEquals("George", instructor.firstMidName)
        assertEquals("Austin", instructor.officeLocation)
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @Test
    fun `query returns instructor data for delete confirmation`() = runTest {
        val id = harness.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin",
            )
        )
        val instructor = harness.query(GetInstructorQuery(id))
        assertNotNull(instructor)
        assertEquals("George", instructor.firstMidName)
        assertEquals("Austin", instructor.officeLocation)
    }

    @Test
    fun `delete instructor removes it from store`() = runTest {
        val id = harness.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
            )
        )
        harness.send(DeleteInstructorCommand(id))
        assertNull(harness.query(GetInstructorQuery(id)))
    }

    @Test
    fun `delete instructor clears department administrator`() = runTest {
        val id = harness.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
            )
        )
        val deptId = harness.send(
            CreateDepartmentCommand(name = "English", budget = 100.0, startDate = "2024-01-01", administratorId = id)
        )
        harness.send(DeleteInstructorCommand(id))
        val dept = harness.query(GetDepartmentQuery(deptId))
        assertNotNull(dept)
        assertNull(dept.administratorId)
    }

    // ── Index ───────────────────────────────────────────────────────────────────

    @Test
    fun `list returns all instructors`() = runTest {
        harness.given(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01", officeLocation = "Austin"
            ),
            CreateEditInstructorCommand(
                lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01", officeLocation = "Houston"
            ),
        )
        val instructors = harness.query(GetInstructorsQuery)
        assertTrue(instructors.size >= 2)
    }
}
