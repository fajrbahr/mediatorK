package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateTests {

    private val fixture = SliceFixture()

    @Test
    fun `should create new course`() = runTest {
        val adminId = fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        val deptId = fixture.insertDepartment(name = "History", administratorId = adminId)

        val command = CreateCourseCommand(
            number = fixture.nextCourseNumber(),
            title = "English 101",
            credits = 4,
            departmentId = deptId,
        )
        val id = fixture.harness.send(command)

        val created = fixture.findCourse(id)
        assertNotNull(created)
        assertEquals(deptId, created.departmentId)
        assertEquals(command.credits, created.credits)
        assertEquals(command.title, created.title)

        // The recording mediator saw exactly the command we sent through the front door.
        assertEquals(command, fixture.harness.sentOf<CreateCourseCommand>().single())
    }
}
