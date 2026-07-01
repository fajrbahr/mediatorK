package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.university.domain.department.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.domain.department.DeleteDepartmentCommand
import com.fajrbahr.mediatork.sample.university.domain.department.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.domain.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.domain.department.EditDepartmentCommand
import com.fajrbahr.mediatork.sample.university.domain.department.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.domain.department.GetDepartmentsQuery
import com.fajrbahr.mediatork.sample.university.domain.instructor.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.domain.instructor.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.domain.instructor.InstructorStore
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
    private val instructorStore = InstructorStore()
    private val mediator = MediatorFactory.create(
        registrars = listOf(
            DepartmentRegistrar(deptStore),
            InstructorRegistrar(instructorStore, deptStore),
        ),
        verifyHandlers = false,
    )

    private suspend fun createAdmin(): Int = mediator.send(
        CreateEditInstructorCommand(lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01")
    )

    // ── Create (Contoso: CreateTests.Should_create_new_department) ────────────

    @Test
    fun `create department returns new id`() = runTest {
        val adminId = createAdmin()
        val id = mediator.send(
            CreateDepartmentCommand(
                name = "Engineering",
                budget = 10.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )
        assertTrue(id > 0)
    }

    @Test
    fun `created department is retrievable with all fields`() = runTest {
        val adminId = createAdmin()
        val id = mediator.send(
            CreateDepartmentCommand(
                name = "Engineering",
                budget = 10.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )
        val dept = mediator.send(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("Engineering", dept.name)
        assertEquals(10.0, dept.budget)
        assertEquals(adminId, dept.administratorId)
    }

    @Test
    fun `create department with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            mediator.send(CreateDepartmentCommand(name = "AB", budget = -1.0, startDate = ""))
        }
    }

    // ── Details (Contoso: DetailsTests.Should_get_department_details) ─────────

    @Test
    fun `query returns department details`() = runTest {
        val adminId = createAdmin()
        val id = mediator.send(
            CreateDepartmentCommand(
                name = "History",
                budget = 123.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )
        val dept = mediator.send(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("History", dept.name)
        assertEquals(adminId, dept.administratorId)
    }

    // ── Edit (Contoso: EditTests) ────────────────────────────────────────────

    @Test
    fun `query returns department data for edit form`() = runTest {
        val adminId = createAdmin()
        val id = mediator.send(
            CreateDepartmentCommand(
                name = "History",
                budget = 123.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )
        val dept = mediator.send(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("History", dept.name)
        assertEquals(adminId, dept.administratorId)
    }

    @Test
    fun `edit department updates all fields`() = runTest {
        val admin1Id = createAdmin()
        val admin2Id = createAdmin()
        val id = mediator.send(
            CreateDepartmentCommand(
                name = "History",
                budget = 123.0,
                startDate = "2024-01-01",
                administratorId = admin1Id
            )
        )
        mediator.send(
            EditDepartmentCommand(
                id = id,
                name = "English",
                budget = 456.0,
                startDate = "2023-06-01",
                administratorId = admin2Id
            )
        )
        val dept = mediator.send(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("English", dept.name)
        assertEquals(456.0, dept.budget)
        assertEquals("2023-06-01", dept.startDate)
        assertEquals(admin2Id, dept.administratorId)
    }

    // ── Delete (Contoso: DeleteTests.Should_delete_department) ───────────────

    @Test
    fun `delete department removes it from store`() = runTest {
        val adminId = createAdmin()
        val id = mediator.send(
            CreateDepartmentCommand(
                name = "History",
                budget = 123.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )
        mediator.send(DeleteDepartmentCommand(id))
        assertNull(mediator.send(GetDepartmentQuery(id)))
    }

    // ── Index (Contoso: IndexTests.Should_list_departments) ──────────────────

    @Test
    fun `list returns all departments`() = runTest {
        val adminId = createAdmin()
        mediator.send(
            CreateDepartmentCommand(
                name = "History",
                budget = 123.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )
        mediator.send(
            CreateDepartmentCommand(
                name = "English",
                budget = 456.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )
        val depts = mediator.send(GetDepartmentsQuery)
        assertTrue(depts.size >= 2)
    }
}
