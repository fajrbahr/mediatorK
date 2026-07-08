package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.detail.DeleteCourseCommand
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get delete details`() = runTest {
        val deptId = fixture.createDepartment(name = "English")
        val id = fixture.createCourse(title = "History 101", credits = 3, departmentId = deptId)

        val result = fixture.harness.query(GetCourseQuery(id))

        assertNotNull(result)
    }

    @Test
    fun `should delete course`() = runTest {
        val deptId = fixture.createDepartment()
        val id = fixture.createCourse(title = "Macro Econ", credits = 3, departmentId = deptId)

        fixture.harness.send(DeleteCourseCommand(id))

        assertNull(fixture.harness.query(GetCourseQuery(id)))
    }

    @Test
    fun `should not throw for non-existent course`() = runTest {
        fixture.harness.send(DeleteCourseCommand(9999))
    }
}
