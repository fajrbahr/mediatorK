package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.university.course.domain.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.domain.InstructorStore
import com.fajrbahr.mediatork.sample.university.student.domain.StudentRegistrar
import com.fajrbahr.mediatork.sample.university.student.domain.StudentStore
import kotlin.test.Test

class HandlerRegistrationTest {

    @Test
    fun `all handlers are registered`() {
        val deptStore = DepartmentStore()
        MediatorFactory.create(
            registrars = listOf(
                CourseRegistrar(CourseStore()),
                DepartmentRegistrar(deptStore),
                StudentRegistrar(StudentStore()),
                InstructorRegistrar(InstructorStore(), deptStore),
            ),
            verifyHandlers = true,
        )
    }
}
