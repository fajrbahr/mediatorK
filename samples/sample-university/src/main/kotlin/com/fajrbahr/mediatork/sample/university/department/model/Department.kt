package com.fajrbahr.mediatork.sample.university.department.model

data class Department(
    val id: Int,
    val name: String,
    val budget: Double,
    val startDate: String,
    val administratorId: Int? = null,
)
