package com.fajrbahr.mediatork.sample.university.course.create


import com.fajrbahr.mediatork.sample.university.SliceFixture
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateCourseViewModelTest {

    val testm = TestMediator()

    @Test
    fun `should create new course`() = runTest {
        testm.on<CreateCourseCommand> return ""

        val v = CreateCourseViewModel(ssdsds)
    }

    @Test
    fun `should return positive id`() = runTest {

    }

    @Test
    fun `should reject invalid data`() = runTest {
    }

    @Test
    fun `should collect all validation errors`() = runTest {

    }
}
