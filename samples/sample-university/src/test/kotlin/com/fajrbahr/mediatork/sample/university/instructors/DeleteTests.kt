package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.delete.DeleteInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.delete.DeleteInstructorQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should query for command`() = runTest {
        val deptId = fixture.insertDepartment(name = "English")
        val courseId = fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)

        val id = fixture.createInstructor(
            lastName = "Costanza", firstMidName = "George",
            officeLocation = "Austin", selectedCourseIds = listOf(courseId),
        )

        val result = fixture.harness.query(DeleteInstructorQuery(id))

        assertNotNull(result)
        assertEquals("George", result.firstMidName)
        assertEquals("Costanza", result.lastName)
        assertEquals("Austin", result.officeLocation)
    }

    @Test
    fun `should delete instructor`() = runTest {
        val instructorId = fixture.createInstructor(
            lastName = "Costanza", firstMidName = "George", officeLocation = "Austin",
        )
        val deptId = fixture.insertDepartment(name = "English", administratorId = instructorId)
        val courseId = fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)

        fixture.harness.send(
            CreateEditInstructorCommand(
                id = instructorId, lastName = "Costanza", firstMidName = "George",
                hireDate = "2024-01-01", officeLocation = "Austin",
                selectedCourseIds = listOf(courseId),
            )
        )

        fixture.harness.send(DeleteInstructorCommand(id = instructorId))

        assertNull(fixture.findInstructor(instructorId))

        val dept = fixture.findDepartment(deptId)
        assertNotNull(dept)
        assertNull(dept.administratorId)
    }
}
