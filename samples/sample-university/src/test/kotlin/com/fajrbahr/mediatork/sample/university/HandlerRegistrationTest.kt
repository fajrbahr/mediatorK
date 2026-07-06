package com.fajrbahr.mediatork.sample.university

import com.fajrbahr.mediatork.buildMediatorK
import com.fajrbahr.mediatork.sample.university.course.domain.*
import com.fajrbahr.mediatork.sample.university.department.domain.*
import com.fajrbahr.mediatork.sample.university.instructor.domain.*
import com.fajrbahr.mediatork.sample.university.student.domain.*
import com.fajrbahr.mediatork.feature.feature
import kotlin.test.Test

class HandlerRegistrationTest {

    @Test
    fun `all features are registered`() {
        val courseStore = CourseStore()
        val deptStore = DepartmentStore()
        val instructorStore = InstructorStore()
        val studentStore = StudentStore()

        buildMediatorK {
            // Course features
            feature(getCourses(courseStore))
            feature(getCourse(courseStore))
            feature(createCourse(courseStore))
            feature(editCourse(courseStore))
            feature(deleteCourse(courseStore))

            // Department features
            feature(getDepartments(deptStore))
            feature(getDepartment(deptStore))
            feature(createDepartment(deptStore))
            feature(editDepartment(deptStore))
            feature(deleteDepartment(deptStore))

            // Instructor features
            feature(getInstructors(instructorStore))
            feature(getInstructor(instructorStore))
            feature(createEditInstructor(instructorStore, deptStore))
            feature(deleteInstructor(instructorStore, deptStore))

            // Student features
            feature(getStudents(studentStore))
            feature(getStudent(studentStore))
            feature(getStudentEnrollments(studentStore))
            feature(createStudent(studentStore))
            feature(editStudent(studentStore))
            feature(deleteStudent(studentStore))
            feature(enrollStudent(studentStore))

            verifyHandlers = true
        }
    }
}
