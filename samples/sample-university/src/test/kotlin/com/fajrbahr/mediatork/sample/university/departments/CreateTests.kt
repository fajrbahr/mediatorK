package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentCommand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create new department`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")

        val command = CreateDepartmentCommand(
            name = "Engineering",
            budget = 10.0,
            startDate = "2024-01-01",
            administratorId = adminId,
        )
        val id = fixture.harness.send(command)

        val created = fixture.findDepartment(id)
        assertNotNull(created)
        assertEquals(command.budget, created.budget)
        assertEquals(command.startDate, created.startDate)
        assertEquals(adminId, created.administratorId)
    }
}
