package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.instructor.domain.GetInstructorQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DetailsTests {

    private val fixture = SliceFixture()

    @Test
    fun `should get instructor details`() = runTest {
        val id = fixture.createInstructor(
            lastName = "Costanza", firstMidName = "George", officeLocation = "Austin"
        )

        val result = fixture.harness.query(GetInstructorQuery(id))

        assertNotNull(result)
        assertEquals("George", result.firstMidName)
        assertEquals("Austin", result.officeLocation)
    }
}
