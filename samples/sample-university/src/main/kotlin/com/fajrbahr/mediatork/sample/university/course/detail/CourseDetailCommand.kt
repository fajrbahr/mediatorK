package com.fajrbahr.mediatork.sample.university.course.detail

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.sample.university.model.Grade

// ── Query ───────────────────────────────────────────────────────────────────

data class GetCourseQuery(val id: Int) : Request<CourseDetailModel?>

data class CourseDetailModel(
    val id: Int,
    val number: Int,
    val title: String,
    val credits: Int,
    val departmentName: String,
    val enrollments: List<EnrollmentModel> = emptyList(),
) {
    data class EnrollmentModel(
        val studentFullName: String,
        val grade: Grade?,
    )
}
