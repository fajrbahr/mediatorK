package com.fajrbahr.mediatork.sample.university.courses

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DetailsTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get course details`() = runTest {
        val deptId = fixture.createDepartment(name = "English")
        val id = fixture.createCourse(title = "English 101", credits = 4, departmentId = deptId)

        val result = fixture.harness.query(GetCourseQuery(id))

        assertNotNull(result)
        assertEquals(id, result.id)
        assertEquals("English 101", result.title)
        assertEquals(4, result.credits)
        assertEquals(deptId, result.departmentId)
    }
}
