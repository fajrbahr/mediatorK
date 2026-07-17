package com.fajrbahr.mediatork.sample.university.course.create

import com.fajrbahr.mediatork.api.Request

data class CreateCourseCommand(
    val number: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentId: Int = 0,
) : Request<Int>
