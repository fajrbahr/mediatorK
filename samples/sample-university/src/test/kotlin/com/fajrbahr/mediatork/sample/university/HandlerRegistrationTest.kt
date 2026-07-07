package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.university.domain.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.domain.CourseStore
import com.fajrbahr.mediatork.sample.university.domain.department.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.domain.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.domain.instructor.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.domain.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.domain.student.StudentRegistrar
import com.fajrbahr.mediatork.sample.university.domain.student.StudentStore
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
