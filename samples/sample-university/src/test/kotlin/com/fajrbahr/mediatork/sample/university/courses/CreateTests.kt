package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create new course`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val deptId = fixture.createDepartment(name = "History", administratorId = adminId)

        val id = fixture.harness.send(
            CreateCourseCommand(
                number = fixture.nextCourseNumber(),
                title = "English 101",
                credits = 4,
                departmentId = deptId
            )
        )

        val created = fixture.harness.query(GetCourseQuery(id))
        assertNotNull(created)
        assertEquals("English 101", created.title)
        assertEquals(4, created.credits)
        assertEquals("History", created.departmentName)
    }
}
