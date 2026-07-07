package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.university.course.domain.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.course.domain.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.domain.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.instructor.domain.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.domain.DeleteInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.domain.GetInstructorQuery
import com.fajrbahr.mediatork.sample.university.instructor.domain.GetInstructorsQuery
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorStore
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstructorIntegrationTest {

    private val instructorStore = InstructorStore()
    private val deptStore = DepartmentStore()
    private val courseStore = CourseStore()
    private val mediator = MediatorFactory.create(
        registrars = listOf(
            InstructorRegistrar(instructorStore, deptStore),
            DepartmentRegistrar(deptStore),
            CourseRegistrar(courseStore),
        ),
        verifyHandlers = false,
    )

    private suspend fun createDept(name: String = "English"): Int =
        mediator.send(CreateDepartmentCommand(name = name, budget = 100.0, startDate = "2024-01-01"))

    @Test
    fun `create instructor returns new id`() = runTest {
        val deptId = createDept()
        val courseId1 =
            mediator.send(CreateCourseCommand(number = 101, title = "English 101", credits = 4, departmentId = deptId))
        val courseId2 =
            mediator.send(CreateCourseCommand(number = 201, title = "English 201", credits = 4, departmentId = deptId))

        val id = mediator.send(
            CreateEditInstructorCommand(
                lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = listOf(courseId1, courseId2),
            )
        )
        val created = mediator.send(GetInstructorQuery(id))
        assertNotNull(created)
        assertEquals("Jerry", created.firstMidName)
        assertEquals("Seinfeld", created.lastName)
        assertEquals("Houston", created.officeLocation)
        assertEquals(2, created.courseIds.size)
    }

    @Test
    fun `create instructor with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            mediator.send(CreateEditInstructorCommand(lastName = "", firstMidName = "", hireDate = ""))
        }
    }

    @Test
    fun `edit instructor updates fields`() = runTest {
        val id = mediator.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin",
            )
        )
        mediator.send(
            CreateEditInstructorCommand(
                id = id, lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = emptyList(),
            )
        )
        val edited = mediator.send(GetInstructorQuery(id))
        assertNotNull(edited)
        assertEquals("Jerry", edited.firstMidName)
        assertEquals("Seinfeld", edited.lastName)
        assertEquals("Houston", edited.officeLocation)
    }

    @Test
    fun `edit instructor merges course assignments`() = runTest {
        val deptId = createDept()
        val courseId1 =
            mediator.send(CreateCourseCommand(number = 301, title = "English 101", credits = 4, departmentId = deptId))
        val courseId2 =
            mediator.send(CreateCourseCommand(number = 302, title = "English 201", credits = 4, departmentId = deptId))

        val id = mediator.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin", selectedCourseIds = listOf(courseId1),
            )
        )
        mediator.send(
            CreateEditInstructorCommand(
                id = id, lastName = "Seinfeld", firstMidName = "Jerry", hireDate = "2024-01-01",
                officeLocation = "Houston", selectedCourseIds = listOf(courseId2),
            )
        )
        val edited = mediator.send(GetInstructorQuery(id))
        assertNotNull(edited)
        assertEquals(1, edited.courseIds.size)
        assertEquals(courseId2, edited.courseIds.first())
    }

    @Test
    fun `query returns instructor details`() = runTest {
        val id = mediator.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin",
            )
        )
        val instructor = mediator.send(GetInstructorQuery(id))
        assertNotNull(instructor)
        assertEquals("George", instructor.firstMidName)
        assertEquals("Austin", instructor.officeLocation)
    }

    @Test
    fun `query returns instructor data for delete confirmation`() = runTest {
        val id = mediator.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
                officeLocation = "Austin",
            )
        )
        val instructor = mediator.send(GetInstructorQuery(id))
        assertNotNull(instructor)
        assertEquals("George", instructor.firstMidName)
        assertEquals("Austin", instructor.officeLocation)
    }

    @Test
    fun `delete instructor removes it from store`() = runTest {
        val id = mediator.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
            )
        )
        mediator.send(DeleteInstructorCommand(id))
        assertNull(mediator.send(GetInstructorQuery(id)))
    }

    @Test
    fun `delete instructor clears department administrator`() = runTest {
        val id = mediator.send(
            CreateEditInstructorCommand(
                lastName = "Costanza", firstMidName = "George", hireDate = "2024-01-01",
            )
        )
        val deptId = mediator.send(
            CreateDepartmentCommand(name = "English", budget = 100.0, startDate = "2024-01-01", administratorId = id)
        )
        mediator.send(DeleteInstructorCommand(id))
        val dept = mediator.send(GetDepartmentQuery(deptId))
        assertNotNull(dept)
        assertNull(dept.administratorId)
    }

    @Test
    fun `list returns all instructors`() = runTest {
        mediator.send(
            CreateEditInstructorCommand(
                lastName = "Costanza",
                firstMidName = "George",
                hireDate = "2024-01-01",
                officeLocation = "Austin"
            )
        )
        mediator.send(
            CreateEditInstructorCommand(
                lastName = "Seinfeld",
                firstMidName = "Jerry",
                hireDate = "2024-01-01",
                officeLocation = "Houston"
            )
        )
        val instructors = mediator.send(GetInstructorsQuery)
        assertTrue(instructors.size >= 2)
    }
}
