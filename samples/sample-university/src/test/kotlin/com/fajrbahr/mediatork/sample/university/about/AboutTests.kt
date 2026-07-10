package com.fajrbahr.mediatork.sample.university.about

import com.fajrbahr.mediatork.handler.query
import com.fajrbahr.mediatork.sample.university.SliceFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AboutTests {

    private val fixture = SliceFixture()

    @Test
    fun `should return empty when no students`() = runTest {
        val fresh = SliceFixture()

        val result = fresh.harness.query(AboutQuery)

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `should group students by enrollment date`() = runTest {
        fixture.insertStudent(lastName = "A", enrollmentDate = "2025-01-01")
        fixture.insertStudent(lastName = "B", enrollmentDate = "2025-01-01")
        fixture.insertStudent(lastName = "C", enrollmentDate = "2025-06-01")

        val result = fixture.harness.query(AboutQuery)

        assertEquals(2, result.items.size)
        val jan = result.items.first { it.enrollmentDate == "2025-01-01" }
        val jun = result.items.first { it.enrollmentDate == "2025-06-01" }
        assertEquals(2, jan.studentCount)
        assertEquals(1, jun.studentCount)
    }
}
