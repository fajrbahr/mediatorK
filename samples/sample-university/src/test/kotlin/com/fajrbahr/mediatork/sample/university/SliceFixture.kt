package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.sample.university.course.domain.*
import com.fajrbahr.mediatork.sample.university.department.domain.*
import com.fajrbahr.mediatork.sample.university.instructor.domain.*
import com.fajrbahr.mediatork.sample.university.student.domain.*

class TestHarness(private val mediator: Mediator) {
    @Suppress("UNCHECKED_CAST")
    suspend fun given(vararg requests: Request<*>) {
        requests.forEach { mediator.send(it as Request<Any?>) }
    }
    suspend fun <T> send(request: Request<T>): T = mediator.send(request)
    suspend fun <T> query(request: Request<T>): T = mediator.send(request)
}

class SliceFixture {

    private val courseStore = CourseStore()
    private val deptStore = DepartmentStore()
    private val instructorStore = InstructorStore()
    private val studentStore = StudentStore()
    private var courseNumberSeq = 1000

    val harness = TestHarness(
        MediatorFactory.create(
            registrars = listOf(
                CourseRegistrar(courseStore),
                DepartmentRegistrar(deptStore),
                InstructorRegistrar(instructorStore, deptStore),
                StudentRegistrar(studentStore),
            ),
        )
    )

    fun nextCourseNumber(): Int = ++courseNumberSeq

    suspend fun createDepartment(
        name: String = "Engineering",
        budget: Double = 100.0,
        startDate: String = "2024-01-01",
        administratorId: Int? = null,
    ): Int = harness.send(
        CreateDepartmentCommand(
            name = name,
            budget = budget,
            startDate = startDate,
            administratorId = administratorId,
        )
    )

    suspend fun createInstructor(
        lastName: String = "Costanza",
        firstMidName: String = "George",
        hireDate: String = "2024-01-01",
        officeLocation: String? = null,
        selectedCourseIds: List<Int> = emptyList(),
    ): Int = harness.send(
        CreateEditInstructorCommand(
            lastName = lastName,
            firstMidName = firstMidName,
            hireDate = hireDate,
            officeLocation = officeLocation,
            selectedCourseIds = selectedCourseIds,
        )
    )

    suspend fun createStudent(
        lastName: String = "Schmoe",
        firstMidName: String = "Joe",
        enrollmentDate: String = "2024-01-01",
    ): Int = harness.send(
        CreateStudentCommand(
            lastName = lastName,
            firstMidName = firstMidName,
            enrollmentDate = enrollmentDate,
        )
    )

    suspend fun createCourse(
        title: String = "Chemistry",
        credits: Int = 3,
        departmentId: Int,
        number: Int = nextCourseNumber(),
    ): Int = harness.send(
        CreateCourseCommand(
            number = number,
            title = title,
            credits = credits,
            departmentId = departmentId,
        )
    )
}
