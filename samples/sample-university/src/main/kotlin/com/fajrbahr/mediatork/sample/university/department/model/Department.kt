package com.fajrbahr.mediatork.sample.university.department.model

import com.fajrbahr.mediatork.sample.university.model.IEntity

data class Department(
    override val id: Int,
    val name: String,
    val budget: Double,
    val startDate: String,
    val administratorId: Int? = null,
) : IEntity
