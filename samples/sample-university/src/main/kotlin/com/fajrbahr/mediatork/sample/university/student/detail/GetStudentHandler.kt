package com.fajrbahr.mediatork.sample.university.student.detail

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun getStudentHandler(
    store: StudentStore,
    courseStore: CourseStore,
): Handler<GetStudentQuery, StudentDetailModel?> = handler@{ request ->
    val student = store.findById(request.id) ?: return@handler null
    val enrollments = store.findEnrollmentsByStudentId(request.id)
    StudentDetailModel(
        id = student.id,
        lastName = student.lastName,
        firstMidName = student.firstMidName,
        enrollmentDate = student.enrollmentDate,
        enrollments = enrollments.map { e ->
            val course = courseStore.findById(e.courseId)
            StudentDetailModel.EnrollmentModel(
                courseId = e.courseId,
                courseTitle = course?.title ?: "Unknown",
                grade = e.grade,
            )
        },
    )
}
