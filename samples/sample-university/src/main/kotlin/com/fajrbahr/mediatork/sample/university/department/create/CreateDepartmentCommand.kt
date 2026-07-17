package com.fajrbahr.mediatork.sample.university.department.create

import com.fajrbahr.mediatork.api.Request

data class CreateDepartmentCommand(
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorId: Int? = null,
) : Request<Int>
