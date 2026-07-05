package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.university.course.domain.courseRegistrar
import com.fajrbahr.mediatork.sample.university.course.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.department.domain.departmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.domain.instructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorStore
import com.fajrbahr.mediatork.sample.university.student.domain.studentRegistrar
import com.fajrbahr.mediatork.sample.university.student.domain.StudentStore
import kotlin.test.Test

class HandlerRegistrationTest {

    @Test
    fun `all features are registered`() {
        val deptStore = DepartmentStore()
        MediatorFactory.create(
            registrars = listOf(
                courseRegistrar(CourseStore()),
                departmentRegistrar(deptStore),
                studentRegistrar(StudentStore()),
                instructorRegistrar(InstructorStore(), deptStore),
            ),
            verifyHandlers = true,
        )
    }
}
