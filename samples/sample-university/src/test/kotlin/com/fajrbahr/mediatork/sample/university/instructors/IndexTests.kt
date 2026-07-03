package com.fajrbahr.mediatork.sample.university.instructors

import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.instructor.domain.GetInstructorsQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class IndexTests {

    private val fixture = SliceFixture()

    @Test
    fun `should list all instructors`() = runTest {
        fixture.createInstructor(lastName = "Costanza", firstMidName = "George")
        fixture.createInstructor(lastName = "Seinfeld", firstMidName = "Jerry")

        val instructors = fixture.harness.query(GetInstructorsQuery)

        assertTrue(instructors.size >= 2)
    }
}
