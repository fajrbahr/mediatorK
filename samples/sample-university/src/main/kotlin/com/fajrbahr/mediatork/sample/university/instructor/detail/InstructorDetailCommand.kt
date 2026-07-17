package com.fajrbahr.mediatork.sample.university.instructor.detail

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class GetInstructorQuery(val id: Int) : Request<InstructorDetailModel?>

data class InstructorDetailModel(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val hireDate: String,
    val officeLocation: String?,
    val courses: List<CourseModel> = emptyList(),
) {
    data class CourseModel(
        val id: Int,
        val title: String,
    )
}
