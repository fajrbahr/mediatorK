package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.detail.DeleteDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.list.GetDepartmentsQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DepartmentIntegrationTest {

    private val fixture = SliceFixture()

    @Test
    fun `create department returns new id`() = runTest {
        val adminId = fixture.createInstructor()
        val id = fixture.harness.send(
            CreateDepartmentCommand(
                name = "Engineering", budget = 10.0, startDate = "2024-01-01", administratorId = adminId
            )
        )
        assertTrue(id > 0)
    }

    @Test
    fun `created department is retrievable with all fields`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val id = fixture.harness.send(
            CreateDepartmentCommand(
                name = "Engineering", budget = 10.0, startDate = "2024-01-01", administratorId = adminId
            )
        )
        val dept = fixture.harness.query(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("Engineering", dept.name)
        assertEquals(10.0, dept.budget)
        assertEquals("Costanza, George", dept.administratorFullName)
    }

    @Test
    fun `create department with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateDepartmentCommand(name = "AB", budget = -1.0, startDate = ""))
        }
    }

    @Test
    fun `query returns department details`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val id = fixture.createDepartment(name = "History", budget = 123.0, administratorId = adminId)
        val dept = fixture.harness.query(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("History", dept.name)
        assertEquals("Costanza, George", dept.administratorFullName)
    }

    @Test
    fun `edit department updates all fields`() = runTest {
        val admin1Id = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val admin2Id = fixture.createInstructor(lastName = "Seinfeld", firstMidName = "Jerry")
        val id = fixture.createDepartment(name = "History", budget = 123.0, administratorId = admin1Id)

        fixture.harness.send(
            EditDepartmentCommand(
                id = id, name = "English", budget = 456.0, startDate = "2023-06-01", administratorId = admin2Id
            )
        )

        val dept = fixture.harness.query(GetDepartmentQuery(id))
        assertNotNull(dept)
        assertEquals("English", dept.name)
        assertEquals(456.0, dept.budget)
        assertEquals("2023-06-01", dept.startDate)
        assertEquals("Seinfeld, Jerry", dept.administratorFullName)
    }

    @Test
    fun `delete department removes it from store`() = runTest {
        val adminId = fixture.createInstructor()
        val id = fixture.createDepartment(name = "History", budget = 123.0, administratorId = adminId)
        fixture.harness.send(DeleteDepartmentCommand(id))
        assertNull(fixture.harness.query(GetDepartmentQuery(id)))
    }

    @Test
    fun `list returns all departments`() = runTest {
        val adminId = fixture.createInstructor()
        fixture.createDepartment(name = "History", administratorId = adminId)
        fixture.createDepartment(name = "English", administratorId = adminId)
        val depts = fixture.harness.query(GetDepartmentsQuery)
        assertTrue(depts.size >= 2)
    }
}
