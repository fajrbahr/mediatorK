package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create new course`() = runTest {
        val deptId = fixture.createDepartment()

        val id = fixture.harness.send(
            CreateCourseCommand(number = 1050, title = "Chemistry", credits = 3, departmentId = deptId)
        )

        val created = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(created)
        assertEquals("Chemistry", created.title)
        assertEquals(3, created.credits)
        assertEquals(deptId, created.departmentId)
    }

    @Test
    fun `should return positive id`() = runTest {
        val deptId = fixture.createDepartment()

        val id = fixture.harness.send(
            CreateCourseCommand(number = 1050, title = "Chemistry", credits = 3, departmentId = deptId)
        )

        assertTrue(id > 0)
    }

    @Test
    fun `should reject invalid data`() = runTest {
        assertFailsWith<ValidationException> {
            fixture.harness.send(CreateCourseCommand(number = 0, title = "", credits = -1))
        }
    }

    @Test
    fun `should collect all validation errors`() = runTest {
        val ex = assertFailsWith<ValidationException> {
            fixture.harness.send(CreateCourseCommand(number = 0, title = "", credits = -1))
        }
        assertTrue(ex.errors.size >= 3)
    }
}
