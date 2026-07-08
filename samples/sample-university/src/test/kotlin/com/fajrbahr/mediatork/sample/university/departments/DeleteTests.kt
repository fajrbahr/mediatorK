package com.fajrbahr.mediatork.sample.university.departments

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.delete.DeleteDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should delete department`() = runTest {
        val adminId = fixture.createInstructor()
        val id = fixture.createDepartment(name = "History", administratorId = adminId)

        fixture.harness.send(DeleteDepartmentCommand(id = id))

        assertNull(fixture.harness.query(GetDepartmentQuery(id)))
    }
}
