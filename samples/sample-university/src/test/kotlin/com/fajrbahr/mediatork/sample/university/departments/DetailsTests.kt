package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DetailsTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get department details`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val id = fixture.insertDepartment(name = "History", budget = 123.0, administratorId = adminId)

        val result = fixture.harness.query(GetDepartmentQuery(id))
        val admin = fixture.findInstructor(adminId)

        assertNotNull(result)
        assertEquals("History", result.name)
        assertEquals(admin!!.fullName, result.administratorFullName)
    }
}
