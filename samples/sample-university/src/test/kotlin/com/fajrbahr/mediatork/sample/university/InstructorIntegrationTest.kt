package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.detail.DeleteInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.detail.GetInstructorQuery
import com.fajrbahr.mediatork.sample.university.instructor.list.GetInstructorsQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstructorIntegrationTest {

    private val fixture = SliceFixture()

    @Test
    fun `create instructor returns new id`() = runTest {
        val deptId = fixture.createDepartment(name = "English")
        val courseId1 = fixture.createCourse(title = "English 101", credits = 4, departmentId = deptId)
        val courseId2 = fixture.createCourse(title = "English 201", credits = 4, departmentId = deptId)

        val id = fixture.harness.send(
            CreateEditInstructorCommand(
                lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = listOf(courseId1, courseId2),
            )
        )
        val created = fixture.harness.query(GetInstructorQuery(id))
        assertNotNull(created)
        assertEquals("Jerry", created.firstMidName)
        assertEquals("Seinfeld", created.lastName)
        assertEquals("Houston", created.officeLocation)
        assertEquals(2, created.courseIds.size)
    }

    @Test
    fun `create instructor with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateEditInstructorCommand(lastName = "", firstMidName = "", hireDate = ""))
        }
    }

    @Test
    fun `edit instructor updates fields`() = runTest {
        val id = fixture.createInstructor(lastName = "Costanza", firstMidName = "George", officeLocation = "Austin")

        fixture.harness.send(
            CreateEditInstructorCommand(
                id = id, lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = emptyList(),
            )
        )

        val edited = fixture.harness.query(GetInstructorQuery(id))
        assertNotNull(edited)
        assertEquals("Jerry", edited.firstMidName)
        assertEquals("Seinfeld", edited.lastName)
        assertEquals("Houston", edited.officeLocation)
    }

    @Test
    fun `edit instructor merges course assignments`() = runTest {
        val deptId = fixture.createDepartment(name = "English")
        val courseId1 = fixture.createCourse(title = "English 101", credits = 4, departmentId = deptId)
        val courseId2 = fixture.createCourse(title = "English 201", credits = 4, departmentId = deptId)

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

        val edited = fixture.harness.query(GetInstructorQuery(id))
        assertNotNull(edited)
        assertEquals(1, edited.courseIds.size)
        assertEquals(courseId2, edited.courseIds.first())
    }

    @Test
    fun `query returns instructor details`() = runTest {
        val id = fixture.createInstructor(lastName = "Costanza", firstMidName = "George", officeLocation = "Austin")
        val instructor = fixture.harness.query(GetInstructorQuery(id))
        assertNotNull(instructor)
        assertEquals("George", instructor.firstMidName)
        assertEquals("Austin", instructor.officeLocation)
    }

    @Test
    fun `delete instructor removes it from store`() = runTest {
        val id = fixture.createInstructor()
        fixture.harness.send(DeleteInstructorCommand(id))
        assertNull(fixture.harness.query(GetInstructorQuery(id)))
    }

    @Test
    fun `delete instructor clears department administrator`() = runTest {
        val id = fixture.createInstructor()
        val deptId = fixture.createDepartment(name = "English", administratorId = id)

        fixture.harness.send(DeleteInstructorCommand(id))

        val dept = fixture.harness.query(GetDepartmentQuery(deptId))
        assertNotNull(dept)
        assertEquals("", dept.administratorFullName)
    }

    @Test
    fun `list returns all instructors`() = runTest {
        fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        fixture.createInstructor(lastName = "Seinfeld", firstMidName = "Jerry")
        val instructors = fixture.harness.query(GetInstructorsQuery)
        assertTrue(instructors.size >= 2)
    }
}
