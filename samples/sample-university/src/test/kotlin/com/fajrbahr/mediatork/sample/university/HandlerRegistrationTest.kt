package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.university.course.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.student.StudentRegistrar
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import kotlin.test.Test

class HandlerRegistrationTest {

    @Test
    fun `all handlers are registered`() {
        val prefs = InMemorySharedPreferences()
        val deptStore = DepartmentStore(prefs)
        MediatorFactory.create(
            registrars = listOf(
                CourseRegistrar(CourseStore(prefs)),
                DepartmentRegistrar(deptStore),
                StudentRegistrar(StudentStore(prefs)),
                InstructorRegistrar(InstructorStore(prefs), deptStore),
            ),
            verifyHandlers = true,
        )
    }
}
