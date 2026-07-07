package com.fajrbahr.mediatork.sample.university.course.model

data class Course(
    val id: Int,
    val number: Int,
    val title: String,
    val credits: Int,
    val departmentId: Int,
)
