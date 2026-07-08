package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.delete.DeleteCourseCommand
import com.fajrbahr.mediatork.sample.university.course.delete.DeleteCourseQuery
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should query for command`() = runTest {
        val deptId = fixture.createDepartment(name = "English")
        val id = fixture.createCourse(title = "History 101", credits = 3, departmentId = deptId)

        val result = fixture.harness.query(DeleteCourseQuery(id))

        assertNotNull(result)
        assertEquals("History 101", result.title)
        assertEquals(3, result.credits)
        assertEquals("English", result.departmentName)
    }

    @Test
    fun `should delete course`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.createCourse(title = "Macro Econ", credits = 3, departmentId = deptId)

        fixture.harness.send(DeleteCourseCommand(id = id))

        assertNull(fixture.harness.query(GetCourseQuery(id)))
    }
}
