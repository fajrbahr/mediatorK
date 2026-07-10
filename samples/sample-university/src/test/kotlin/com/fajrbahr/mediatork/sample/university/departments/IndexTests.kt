package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.list.GetDepartmentsQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should list departments`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val deptId1 = fixture.insertDepartment(name = "History", administratorId = adminId)
        val deptId2 = fixture.insertDepartment(name = "English", administratorId = adminId)

        val result = fixture.harness.query(GetDepartmentsQuery)

        assertTrue(result.size >= 2)
        assertTrue(result.any { it.id == deptId1 })
        assertTrue(result.any { it.id == deptId2 })
    }
}
