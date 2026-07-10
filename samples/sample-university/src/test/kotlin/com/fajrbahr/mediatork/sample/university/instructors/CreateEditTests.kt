package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorCommand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateEditTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create new instructor`() = runTest {
        val deptId = fixture.insertDepartment(name = "English")
        val courseId1 = fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)
        val courseId2 = fixture.insertCourse(title = "English 201", credits = 4, departmentId = deptId)

        val command = CreateEditInstructorCommand(
            lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
            officeLocation = "Houston", selectedCourseIds = listOf(courseId1, courseId2),
        )
        val id = fixture.harness.send(command)

        val created = fixture.findInstructor(id)
        assertNotNull(created)
        assertEquals(command.firstMidName, created.firstMidName)
        assertEquals(command.lastName, created.lastName)
        assertEquals(command.hireDate, created.hireDate)
        assertEquals(command.officeLocation, created.officeLocation)
        assertEquals(2, created.courseIds.size)
    }

    @Test
    fun `should edit instructor details`() = runTest {
        val deptId = fixture.insertDepartment(name = "English")
        fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)
        fixture.insertCourse(title = "English 201", credits = 4, departmentId = deptId)

        val id = fixture.createInstructor(
            lastName = "Costanza", firstMidName = "George", officeLocation = "Austin",
        )

        val command = CreateEditInstructorCommand(
            id = id, lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
            officeLocation = "Houston", selectedCourseIds = emptyList(),
        )
        fixture.harness.send(command)

        val edited = fixture.findInstructor(id)
        assertNotNull(edited)
        assertEquals(command.firstMidName, edited.firstMidName)
        assertEquals(command.lastName, edited.lastName)
        assertEquals(command.officeLocation, edited.officeLocation)
    }

    @Test
    fun `should merge course assignments`() = runTest {
        val deptId = fixture.insertDepartment(name = "English")
        val courseId1 = fixture.insertCourse(title = "English 101", credits = 4, departmentId = deptId)
        val courseId2 = fixture.insertCourse(title = "English 201", credits = 4, departmentId = deptId)

        val id = fixture.harness.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin", selectedCourseIds = listOf(courseId1),
            )
        )

        fixture.harness.send(
            CreateEditInstructorCommand(
                id = id, lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = listOf(courseId2),
            )
        )

        val edited = fixture.findInstructor(id)
        assertNotNull(edited)
        assertEquals("Jerry", edited.firstMidName)
        assertEquals("Seinfeld", edited.lastName)
        assertEquals("Houston", edited.officeLocation)
        assertEquals(1, edited.courseIds.size)
        assertEquals(courseId2, edited.courseIds.first())
    }
}
