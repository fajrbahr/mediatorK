package com.fajrbahr.mediatork.sample.university.course.create

import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import com.fajrbahr.mediatork.test.StubMediator
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateCourseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mediator = StubMediator()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should create new course`() = runTest {
        mediator.on<CreateCourseCommand>() returns 42

        val vm = CreateCourseViewModel(mediator)
        vm.onNumberChange("101")
        vm.onTitleChange("Chemistry")
        vm.onCreditsChange("3")
        vm.onDepartmentIdChange(1)

        vm.submit()

        assertTrue(vm.state.value.isSaved)
        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test
    fun `should return positive id`() = runTest {
        mediator.on<CreateCourseCommand>() answers { cmd ->
            require(cmd.number > 0)
            cmd.number
        }

        val vm = CreateCourseViewModel(mediator)
        vm.onNumberChange("500")
        vm.onTitleChange("Physics")
        vm.onCreditsChange("4")
        vm.onDepartmentIdChange(1)

        vm.submit()

        assertTrue(vm.state.value.isSaved)
    }

    @Test
    fun `should reject invalid data`() = runTest {
        mediator.on<CreateCourseCommand>() throws ValidationException(
            listOf("Title must be between 3 and 50 characters")
        )

        val vm = CreateCourseViewModel(mediator)
        vm.onNumberChange("101")
        vm.onTitleChange("AB")
        vm.onCreditsChange("3")
        vm.onDepartmentIdChange(1)

        vm.submit()

        assertFalse(vm.state.value.isSaved)
        assertTrue(vm.state.value.errors.isNotEmpty())
    }

    @Test
    fun `should collect all validation errors`() = runTest {
        mediator.on<CreateCourseCommand>() throws ValidationException(
            listOf(
                "Number must be greater than 0",
                "Title must be between 3 and 50 characters",
                "Credits must be between 0 and 5",
            )
        )

        val vm = CreateCourseViewModel(mediator)
        vm.submit()

        assertFalse(vm.state.value.isSaved)
        assertEquals(3, vm.state.value.errors.size)
    }
}
