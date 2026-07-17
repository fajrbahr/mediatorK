package com.fajrbahr.mediatork.sample.university.course.detail

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun getCourseHandler(
    store: CourseStore,
    departmentStore: DepartmentStore,
    studentStore: StudentStore,
): Handler<GetCourseQuery, CourseDetailModel?> = handler@{ request ->
    val course = store.findById(request.id) ?: return@handler null
    val department = departmentStore.findById(course.departmentId)
    val enrollments = studentStore.findEnrollmentsByCourseId(course.id)
    CourseDetailModel(
        id = course.id,
        number = course.number,
        title = course.title,
        credits = course.credits,
        departmentName = department?.name ?: "",
        enrollments = enrollments.map { e ->
            val student = studentStore.findById(e.studentId)
            CourseDetailModel.EnrollmentModel(
                studentFullName = student?.fullName ?: "Unknown",
                grade = e.grade,
            )
        },
    )
}
