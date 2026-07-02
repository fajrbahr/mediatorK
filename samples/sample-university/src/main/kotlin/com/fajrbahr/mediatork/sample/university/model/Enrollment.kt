package com.fajrbahr.mediatork.sample.university.model

data class Enrollment(
    override val id: Int,
    val courseId: Int,
    val studentId: Int,
    val grade: Grade? = null,
) : IEntity
