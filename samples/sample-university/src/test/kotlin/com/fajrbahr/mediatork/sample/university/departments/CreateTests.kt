package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create new department`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")

        val id = fixture.harness.send(
            CreateDepartmentCommand(
                name = "Engineering",
                budget = 10.0,
                startDate = "2024-01-01",
                administratorId = adminId
            )
        )

        val created = fixture.harness.query(GetDepartmentQuery(id))
        assertNotNull(created)
        assertEquals("Engineering", created.name)
        assertEquals(10.0, created.budget)
        assertEquals("2024-01-01", created.startDate)
        assertEquals("Costanza, George", created.administratorFullName)
    }
}
