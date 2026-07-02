package com.fajrbahr.mediatork.sample.university.course.model

import com.fajrbahr.mediatork.sample.university.model.IEntity

data class Course(
    override val id: Int,
    val number: Int,
    val title: String,
    val credits: Int,
    val departmentId: Int,
) : IEntity
