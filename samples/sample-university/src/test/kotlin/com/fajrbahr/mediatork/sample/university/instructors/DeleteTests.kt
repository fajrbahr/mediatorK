package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.instructor.delete.DeleteInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.delete.DeleteInstructorQuery
import com.fajrbahr.mediatork.sample.university.instructor.detail.GetInstructorQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should query for command`() = runTest {
        val id = fixture.createInstructor(lastName = "Costanza", firstMidName = "George", officeLocation = "Austin")

        val result = fixture.harness.query(DeleteInstructorQuery(id))

        assertNotNull(result)
        assertEquals("George", result.firstMidName)
        assertEquals("Costanza", result.lastName)
        assertEquals("Austin", result.officeLocation)
    }

    @Test
    fun `should delete instructor`() = runTest {
        val instructorId = fixture.createInstructor()
        val deptId = fixture.createDepartment(name = "English", administratorId = instructorId)

        fixture.harness.send(DeleteInstructorCommand(id = instructorId))

        assertNull(fixture.harness.query(GetInstructorQuery(instructorId)))
        val dept = fixture.harness.query(GetDepartmentQuery(deptId))
        assertNotNull(dept)
        assertEquals("", dept.administratorFullName)
    }
}
