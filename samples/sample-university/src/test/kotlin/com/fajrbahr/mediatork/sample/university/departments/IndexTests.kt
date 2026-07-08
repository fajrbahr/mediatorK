package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.list.GetDepartmentsQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should list departments`() = runTest {
        val adminId = fixture.createInstructor()
        fixture.createDepartment(name = "History", administratorId = adminId)
        fixture.createDepartment(name = "English", administratorId = adminId)

        val depts = fixture.harness.query(GetDepartmentsQuery)

        assertTrue(depts.size >= 2)
    }
}
