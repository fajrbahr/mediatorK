package com.fajrbahr.mediatork.sample.university.students

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.student.list.GetStudentsQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should return all items for default search`() = runTest {
        val id1 = fixture.insertStudent(lastName = "Schmoe", firstMidName = "Joe")
        val id2 = fixture.insertStudent(lastName = "Schmoe", firstMidName = "Jane")

        val result = fixture.harness.query(GetStudentsQuery)

        assertTrue(result.size >= 2)
        assertTrue(result.any { it.id == id1 })
        assertTrue(result.any { it.id == id2 })
    }

    @Test
    fun `should sort based on name`() = runTest {
        fixture.insertStudent(lastName = "Zeta", firstMidName = "Joe")
        fixture.insertStudent(lastName = "Alpha", firstMidName = "Jane")

        val result = fixture.harness.query(GetStudentsQuery)

        assertEquals("Alpha", result.first().lastName)
    }
}
