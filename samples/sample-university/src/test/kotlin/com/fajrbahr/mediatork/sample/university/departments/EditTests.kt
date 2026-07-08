package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EditTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get edit details`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val id = fixture.createDepartment(name = "History", budget = 123.0, administratorId = adminId)

        val dept = fixture.harness.query(GetDepartmentQuery(id))

        assertNotNull(dept)
        assertEquals("History", dept.name)
        assertEquals("Costanza, George", dept.administratorFullName)
    }

    @Test
    fun `should edit department`() = runTest {
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
}
