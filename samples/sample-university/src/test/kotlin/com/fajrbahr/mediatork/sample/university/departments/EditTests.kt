package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentCommand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EditTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get edit department details`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val id = fixture.insertDepartment(name = "History", budget = 123.0, administratorId = adminId)

        val result = fixture.harness.query(GetDepartmentQuery(id))

        assertNotNull(result)
        assertEquals("History", result.name)
        assertEquals("Costanza, George", result.administratorFullName)
    }

    @Test
    fun `should edit department`() = runTest {
        val admin1Id = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val admin2Id = fixture.createInstructor(lastName = "Seinfeld", firstMidName = "Jerry")
        val id = fixture.insertDepartment(name = "History", budget = 123.0, administratorId = admin1Id)

        val command = EditDepartmentCommand(
            id = id, name = "English", budget = 456.0, startDate = "2023-06-01", administratorId = admin2Id,
        )
        fixture.harness.send(command)

        val edited = fixture.findDepartment(id)
        assertNotNull(edited)
        assertEquals(command.name, edited.name)
        assertEquals(admin2Id, edited.administratorId)
        assertEquals(command.startDate, edited.startDate)
        assertEquals(command.budget, edited.budget)
    }
}
