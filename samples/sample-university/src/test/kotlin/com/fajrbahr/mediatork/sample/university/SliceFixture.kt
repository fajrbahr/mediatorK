package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.sample.university.course.domain.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.course.domain.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.domain.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorStore
import com.fajrbahr.mediatork.sample.university.student.domain.CreateStudentCommand
import com.fajrbahr.mediatork.sample.university.student.domain.StudentRegistrar
import com.fajrbahr.mediatork.sample.university.student.domain.StudentStore
import com.fajrbahr.mediatork.test.HandlerTestHarness
import com.fajrbahr.mediatork.test.buildHandlerTestHarness

class SliceFixture {

    private val courseStore = CourseStore()
    private val deptStore = DepartmentStore()
    private val instructorStore = InstructorStore()
    private val studentStore = StudentStore()
    private var courseNumberSeq = 1000

    val harness: HandlerTestHarness = buildHandlerTestHarness(
        registrars = listOf(
            CourseRegistrar(courseStore),
            DepartmentRegistrar(deptStore),
            InstructorRegistrar(instructorStore, deptStore),
            StudentRegistrar(studentStore),
        ),
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
