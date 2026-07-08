package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.detail.GetInstructorQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CreateEditTests {

    private val fixture = SliceFixture()

    // ── Create ──────────────────────────────────────────────────────────────────

    @Test
    fun `should create new instructor`() = runTest {
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
    fun `should reject invalid data`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateEditInstructorCommand(lastName = "", firstMidName = "", hireDate = ""))
        }
    }

    // ── Edit ────────────────────────────────────────────────────────────────────

    @Test
    fun `should edit instructor details`() = runTest {
        val id = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")

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

    // ── Merge courses ───────────────────────────────────────────────────────────

    @Test
    fun `should merge course assignments`() = runTest {
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
}
