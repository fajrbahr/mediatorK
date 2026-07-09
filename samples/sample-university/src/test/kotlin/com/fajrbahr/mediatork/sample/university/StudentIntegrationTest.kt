package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentCommand
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentCommand
import com.fajrbahr.mediatork.sample.university.student.detail.EnrollStudentCommand
import com.fajrbahr.mediatork.sample.university.student.detail.GetStudentQuery
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentCommand
import com.fajrbahr.mediatork.sample.university.student.list.GetStudentsQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudentIntegrationTest {

    private val fixture = SliceFixture()

    @Test
    fun `create student returns new id`() = runTest {
        val id = fixture.harness.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        assertTrue(id > 0)
    }

    @Test
    fun `created student is retrievable with all fields`() = runTest {
        val id = fixture.harness.send(
            CreateStudentCommand(lastName = "Schmoe", firstMidName = "Joe", enrollmentDate = "2024-01-01")
        )
        val student = fixture.harness.query(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Joe", student.firstMidName)
        assertEquals("Schmoe", student.lastName)
        assertEquals("2024-01-01", student.enrollmentDate)
    }

    @Test
    fun `create student with invalid data throws ValidationException`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateStudentCommand(lastName = "", firstMidName = "", enrollmentDate = ""))
        }
    }

    @Test
    fun `details includes enrollments`() = runTest {
        val deptId = fixture.createDepartment()
        val courseId1 = fixture.createCourse(title = "Course 1", credits = 4, departmentId = deptId)
        val courseId2 = fixture.createCourse(title = "Course 2", credits = 4, departmentId = deptId)
        val studentId = fixture.createStudent()

        fixture.harness.send(EnrollStudentCommand(studentId = studentId, courseId = courseId1, grade = Grade.A))
        fixture.harness.send(EnrollStudentCommand(studentId = studentId, courseId = courseId2, grade = Grade.F))

        val student = fixture.harness.query(GetStudentQuery(studentId))
        assertNotNull(student)
        assertEquals(2, student.enrollments.size)
    }

    @Test
    fun `edit student updates fields`() = runTest {
        val id = fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe")

        fixture.harness.send(
            EditStudentCommand(id = id, lastName = "Smith", firstMidName = "Mary", enrollmentDate = "2023-01-01")
        )

        val student = fixture.harness.query(GetStudentQuery(id))
        assertNotNull(student)
        assertEquals("Mary", student.firstMidName)
        assertEquals("Smith", student.lastName)
        assertEquals("2023-01-01", student.enrollmentDate)
    }

    @Test
    fun `delete student removes it from store`() = runTest {
        val id = fixture.createStudent()
        fixture.harness.send(DeleteStudentCommand(id))
        assertNull(fixture.harness.query(GetStudentQuery(id)))
    }

    @Test
    fun `list returns all students`() = runTest {
        fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe")
        fixture.createStudent(lastName = "Schmoe", firstMidName = "Jane")
        val students = fixture.harness.query(GetStudentsQuery)
        assertTrue(students.size >= 2)
    }

    @Test
    fun `list returns students sorted by last name`() = runTest {
        fixture.createStudent(lastName = "Zeta", firstMidName = "Joe")
        fixture.createStudent(lastName = "Alpha", firstMidName = "Jane")
        val students = fixture.harness.query(GetStudentsQuery)
        assertEquals("Alpha", students.first().lastName)
    }
}
