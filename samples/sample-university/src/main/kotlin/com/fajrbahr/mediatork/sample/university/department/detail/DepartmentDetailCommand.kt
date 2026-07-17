package com.fajrbahr.mediatork.sample.university.department.detail

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class GetDepartmentQuery(val id: Int) : Request<DepartmentDetailModel?>

data class DepartmentDetailModel(
    val id: Int,
    val name: String,
    val budget: Double,
    val startDate: String,
    val administratorFullName: String,
    val courses: List<CourseModel> = emptyList(),
) {
    data class CourseModel(
        val id: Int,
        val title: String,
        val credits: Int,
    )
}
