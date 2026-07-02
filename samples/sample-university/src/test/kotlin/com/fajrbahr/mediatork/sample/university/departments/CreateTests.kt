package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.GetDepartmentQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create new department`() = runTest {
        val adminId = fixture.createInstructor()

        val id = fixture.harness.send(
            CreateDepartmentCommand(
                name = "Engineering", budget = 10.0, startDate = "2024-01-01", administratorId = adminId
            )
        )

        val created = fixture.harness.query(GetDepartmentQuery(id))
        assertNotNull(created)
        assertEquals("Engineering", created.name)
        assertEquals(10.0, created.budget)
        assertEquals(adminId, created.administratorId)
    }

    @Test
    fun `should return positive id`() = runTest {
        val adminId = fixture.createInstructor()

        val id = fixture.createDepartment(name = "Engineering", administratorId = adminId)

        assertTrue(id > 0)
    }

    @Test
    fun `should reject invalid data`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateDepartmentCommand(name = "AB", budget = -1.0, startDate = ""))
        }
    }
}
