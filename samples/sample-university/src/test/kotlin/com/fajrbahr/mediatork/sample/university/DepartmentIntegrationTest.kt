package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.DeleteDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.domain.EditDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.department.domain.GetDepartmentsQuery
import com.fajrbahr.mediatork.sample.university.instructor.domain.CreateEditInstructorCommand
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

class DepartmentIntegrationTest {

    private val deptStore = DepartmentStore()
    private val harness = buildHandlerTestHarness(
        registrars = listOf(
            DepartmentRegistrar(deptStore),
            InstructorRegistrar(InstructorStore(), deptStore),
        ),
    )

    private suspend fun createAdmin(): Int = harness.send(
        CreateEditInstructorCommand(lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01")
    )

    // ── Create ──────────────────────────────────────────────────────────────────

    @Test
    fun `create department returns new id`() = runTest {
        val adminId = createAdmin()
        val id = harness.send(
            CreateDepartmentCommand(
                name = "Engineering", budget = 10.0, startDate = "2024-01-01", administratorId = adminId
            )
        )
        assertTrue(id > 0)
    }

    @Test
    fun `created department is retrievable with all fields`() = runTest {
        val adminId = createAdmin()
        val id = harness.send(
            CreateDepartmentCommand(
                name = "Engineering", budget = 10.0, startDate = "2024-01-01", administratorId = adminId
            )
        )
        val dept = harness.query(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("Engineering", dept.name)
        assertEquals(10.0, dept.budget)
        assertEquals(adminId, dept.administratorId)
    }

    @Test
    fun `create department with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            harness.send(CreateDepartmentCommand(name = "AB", budget = -1.0, startDate = ""))
        }
    }

    // ── Details ─────────────────────────────────────────────────────────────────

    @Test
    fun `query returns department details`() = runTest {
        val adminId = createAdmin()
        val id = harness.send(
            CreateDepartmentCommand(
                name = "History", budget = 123.0, startDate = "2024-01-01", administratorId = adminId
            )
        )
        val dept = harness.query(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("History", dept.name)
        assertEquals(adminId, dept.administratorId)
    }

    // ── Edit ────────────────────────────────────────────────────────────────────

    @Test
    fun `query returns department data for edit form`() = runTest {
        val adminId = createAdmin()
        val id = harness.send(
            CreateDepartmentCommand(
                name = "History", budget = 123.0, startDate = "2024-01-01", administratorId = adminId
            )
        )
        val dept = harness.query(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("History", dept.name)
        assertEquals(adminId, dept.administratorId)
    }

    @Test
    fun `edit department updates all fields`() = runTest {
        val admin1Id = createAdmin()
        val admin2Id = createAdmin()
        val id = harness.send(
            CreateDepartmentCommand(
                name = "History", budget = 123.0, startDate = "2024-01-01", administratorId = admin1Id
            )
        )
        harness.send(
            EditDepartmentCommand(
                id = id, name = "English", budget = 456.0, startDate = "2023-06-01", administratorId = admin2Id
            )
        )
        val dept = harness.query(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("English", dept.name)
        assertEquals(456.0, dept.budget)
        assertEquals("2023-06-01", dept.startDate)
        assertEquals(admin2Id, dept.administratorId)
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @Test
    fun `delete department removes it from store`() = runTest {
        val adminId = createAdmin()
        val id = harness.send(
            CreateDepartmentCommand(
                name = "History", budget = 123.0, startDate = "2024-01-01", administratorId = adminId
            )
        )
        harness.send(DeleteDepartmentCommand(id))
        assertNull(harness.query(GetDepartmentQuery(id)))
    }

    // ── Index ───────────────────────────────────────────────────────────────────

    @Test
    fun `list returns all departments`() = runTest {
        val adminId = createAdmin()
        harness.given(
            CreateDepartmentCommand(name = "History", budget = 123.0, startDate = "2024-01-01", administratorId = adminId),
            CreateDepartmentCommand(name = "English", budget = 456.0, startDate = "2024-01-01", administratorId = adminId),
        )
        val depts = harness.query(GetDepartmentsQuery)
        assertTrue(depts.size >= 2)
    }
}
