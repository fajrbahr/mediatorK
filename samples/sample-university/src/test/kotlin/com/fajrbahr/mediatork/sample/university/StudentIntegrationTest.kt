package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.university.course.domain.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.course.domain.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.student.domain.CreateStudentCommand
import com.fajrbahr.mediatork.sample.university.student.domain.DeleteStudentCommand
import com.fajrbahr.mediatork.sample.university.student.domain.EditStudentCommand
import com.fajrbahr.mediatork.sample.university.student.domain.EnrollStudentCommand
import com.fajrbahr.mediatork.sample.university.student.domain.GetStudentEnrollmentsQuery
import com.fajrbahr.mediatork.sample.university.student.domain.GetStudentQuery
import com.fajrbahr.mediatork.sample.university.student.domain.GetStudentsQuery
import com.fajrbahr.mediatork.sample.university.student.domain.StudentRegistrar
import com.fajrbahr.mediatork.sample.university.student.domain.StudentStore
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudentIntegrationTest {

    private val studentStore = StudentStore()
    private val courseStore = CourseStore()
    private val deptStore = DepartmentStore()
    private val mediator = MediatorFactory.create(
        registrars = listOf(
            StudentRegistrar(studentStore),
            CourseRegistrar(courseStore),
            DepartmentRegistrar(deptStore),
        ),
        verifyHandlers = false,
    )

    private suspend fun createDept(): Int =
        mediator.send(CreateDepartmentCommand(name = "English", budget = 100.0, startDate = "2024-01-01"))

    @Test
    fun `create student returns new id`() = runTest {
        val id = mediator.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        assertTrue(id > 0)
    }

    @Test
    fun `created student is retrievable with all fields`() = runTest {
        val id = mediator.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        val student = mediator.send(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Joe", student.firstMidName)
        assertEquals("Schmoe", student.lastName)
        assertEquals("2024-01-01", student.enrollmentDate)
    }

    @Test
    fun `create student with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            mediator.send(CreateStudentCommand(lastName = "", firstMidName = "", enrollmentDate = ""))
        }
    }

    @Test
    fun `details includes enrollments`() = runTest {
        val deptId = createDept()
        val courseId1 =
            mediator.send(CreateCourseCommand(number = 101, title = "Course 1", credits = 4, departmentId = deptId))
        val courseId2 =
            mediator.send(CreateCourseCommand(number = 102, title = "Course 2", credits = 4, departmentId = deptId))
        val studentId = mediator.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2013-01-01")
        )
        mediator.send(EnrollStudentCommand(studentId = studentId, courseId = courseId1, grade = Grade.A))
        mediator.send(EnrollStudentCommand(studentId = studentId, courseId = courseId2, grade = Grade.F))

        val enrollments = mediator.send(GetStudentEnrollmentsQuery(studentId))
        assertEquals(2, enrollments.size)
    }

    @Test
    fun `query returns student data for edit form`() = runTest {
        val id = mediator.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        val student = mediator.send(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Joe", student.firstMidName)
        assertEquals("Schmoe", student.lastName)
        assertEquals("2024-01-01", student.enrollmentDate)
    }

    @Test
    fun `edit student updates fields`() = runTest {
        val id = mediator.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        mediator.send(
            EditStudentCommand(id = id, lastName = "Smith", firstMidName = "Mary", enrollmentDate = "2023-01-01")
        )
        val student = mediator.send(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Mary", student.firstMidName)
        assertEquals("Smith", student.lastName)
        assertEquals("2023-01-01", student.enrollmentDate)
    }

    @Test
    fun `query returns student data for delete confirmation`() = runTest {
        val id = mediator.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        val student = mediator.send(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Joe", student.firstMidName)
        assertEquals("Schmoe", student.lastName)
    }

    @Test
    fun `delete student removes it from store`() = runTest {
        val id = mediator.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        mediator.send(DeleteStudentCommand(id))
        assertNull(mediator.send(GetStudentQuery(id)))
    }

    @Test
    fun `list returns all students`() = runTest {
        mediator.send(CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01"))
        mediator.send(CreateStudentCommand(lastName = "Schmoe", firstMidName = "Jane", enrollmentDate = "2024-01-01"))
        val students = mediator.send(GetStudentsQuery)
        assertTrue(students.size >= 2)
    }

    @Test
    fun `list returns students sorted by last name`() = runTest {
        mediator.send(CreateStudentCommand(lastName = "Zeta", firstMidName = "Joe", enrollmentDate = "2024-01-01"))
        mediator.send(CreateStudentCommand(lastName = "Alpha", firstMidName = "Jane", enrollmentDate = "2024-01-01"))
        val students = mediator.send(GetStudentsQuery)
        assertEquals("Alpha", students.first().lastName)
    }
}
