package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.instructor.detail.DeleteInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.detail.GetInstructorQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get delete details`() = runTest {
        val id = fixture.createInstructor(lastName = "Costanza", firstMidName = "George", officeLocation = "Austin")

        val result = fixture.harness.query(GetInstructorQuery(id))

        assertNotNull(result)
    }

    @Test
    fun `should delete instructor`() = runTest {
        val id = fixture.createInstructor()

        fixture.harness.send(DeleteInstructorCommand(id))

        assertNull(fixture.harness.query(GetInstructorQuery(id)))
    }

    @Test
    fun `should clear department administrator on delete`() = runTest {
        val instructorId = fixture.createInstructor()
        val deptId = fixture.createDepartment(name = "English", administratorId = instructorId)

        fixture.harness.send(DeleteInstructorCommand(instructorId))

        val dept = fixture.harness.query(GetDepartmentQuery(deptId))
        assertNotNull(dept)
        assertNull(dept.administratorId)
    }
}
