package com.fajrbahr.mediatork.sample.university.instructor.list

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun getInstructorsHandler(
    store: InstructorStore,
    departmentStore: DepartmentStore,
    courseStore: CourseStore,
    studentStore: StudentStore,
): Handler<GetInstructorsQuery, InstructorIndexModel> = { request ->
    val instructors = store.findAll().map { instructor ->
        InstructorIndexModel.InstructorRow(
            id = instructor.id,
            lastName = instructor.lastName,
            firstMidName = instructor.firstMidName,
            hireDate = instructor.hireDate,
            officeLocation = instructor.officeLocation,
        )
    }

    val courses = if (request.selectedInstructorId != null) {
        val instructor = store.findById(request.selectedInstructorId)
        instructor?.courseIds?.mapNotNull { courseId ->
            val course = courseStore.findById(courseId) ?: return@mapNotNull null
            val dept = departmentStore.findById(course.departmentId)
            InstructorIndexModel.CourseRow(
                id = course.id,
                number = course.number,
                title = course.title,
                departmentName = dept?.name ?: "",
            )
        } ?: emptyList()
    } else {
        emptyList()
    }

    val enrollments = if (request.selectedCourseId != null) {
        studentStore.findEnrollmentsByCourseId(request.selectedCourseId).map { enrollment ->
            val student = studentStore.findById(enrollment.studentId)
            InstructorIndexModel.EnrollmentRow(
                studentFullName = student?.fullName ?: "Unknown",
                grade = enrollment.grade,
            )
        }
    } else {
        emptyList()
    }

    InstructorIndexModel(
        instructors = instructors,
        courses = courses,
        enrollments = enrollments,
        selectedInstructorId = request.selectedInstructorId,
        selectedCourseId = request.selectedCourseId,
    )
}
