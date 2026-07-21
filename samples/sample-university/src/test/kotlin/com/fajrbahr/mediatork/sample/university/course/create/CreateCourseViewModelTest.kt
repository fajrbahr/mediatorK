package com.fajrbahr.mediatork.sample.university.course.create

import com.fajrbahr.mediatork.sample.university.InMemorySharedPreferences
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.test.testMediator
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

    // No fakes: the real handler persists to a real store, the real validator rejects bad input.
    // testMediator also records what the ViewModel dispatched so we can assert the mapping.
    private val store = CourseStore(InMemorySharedPreferences())
    private val mediator = testMediator {
        handle(createCourseHandler(store))
        validate(createCourseValidator)
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should map ui state to command and persist the course`() = runTest {
        val vm = CreateCourseViewModel(mediator)
        vm.onNumberChange("101")
        vm.onTitleChange("Chemistry")
        vm.onCreditsChange("3")
        vm.onDepartmentIdChange(1)

        vm.submit()

        assertTrue(vm.state.value.isSaved)
        assertTrue(vm.state.value.errors.isEmpty())

        // The ViewModel dispatched exactly the command the form described.
        val command = mediator.sentOf<CreateCourseCommand>().single()
        assertEquals(CreateCourseCommand(number = 101, title = "Chemistry", credits = 3, departmentId = 1), command)

        // The real handler actually persisted it.
        val created = store.findAll().single()
        assertEquals(101, created.number)
        assertEquals("Chemistry", created.title)
        assertEquals(3, created.credits)
        assertEquals(1, created.departmentId)
    }

    @Test
    fun `should reject invalid data without persisting`() = runTest {
        val vm = CreateCourseViewModel(mediator)
        vm.onNumberChange("101")
        vm.onTitleChange("AB") // too short for the real validator
        vm.onCreditsChange("3")
        vm.onDepartmentIdChange(1)

        vm.submit()

        assertFalse(vm.state.value.isSaved)
        assertTrue(vm.state.value.errors.isNotEmpty())
        assertTrue(store.findAll().isEmpty())
    }

    @Test
    fun `should collect all validation errors from the real validator`() = runTest {
        val vm = CreateCourseViewModel(mediator)

        vm.submit() // empty form: number, title and credits all invalid

        assertFalse(vm.state.value.isSaved)
        assertEquals(3, vm.state.value.errors.size)
        assertTrue(store.findAll().isEmpty())
    }
}
