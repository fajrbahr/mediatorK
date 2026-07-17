package com.fajrbahr.mediatork.sample.university.course.list

import com.fajrbahr.mediatork.api.Request

data object GetCoursesQuery : Request<GetCoursesResult>

data class GetCoursesResult(
    val courses: List<CourseListModel>,
)

data class CourseListModel(
    val id: Int,
    val title: String,
    val credits: Int,
    val departmentName: String,
)
