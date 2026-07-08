package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.list.GetStudentsQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should return all students`() = runTest {
        fixture.createStudent(lastName = "Schmoe", firstMidName = "Joe")
        fixture.createStudent(lastName = "Schmoe", firstMidName = "Jane")

        val students = fixture.harness.query(GetStudentsQuery)

        assertTrue(students.size >= 2)
    }

    @Test
    fun `should return students sorted by last name`() = runTest {
        fixture.createStudent(lastName = "Zeta", firstMidName = "Joe")
        fixture.createStudent(lastName = "Alpha", firstMidName = "Jane")

        val students = fixture.harness.query(GetStudentsQuery)

        assertEquals("Alpha", students.first().lastName)
    }
}
