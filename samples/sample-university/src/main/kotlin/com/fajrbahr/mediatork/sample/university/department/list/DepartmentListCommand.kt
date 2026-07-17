package com.fajrbahr.mediatork.sample.university.department.list

import com.fajrbahr.mediatork.api.Request

data object GetDepartmentsQuery : Request<List<DepartmentListModel>>

data class DepartmentListModel(
    val id: Int,
    val name: String,
    val budget: Double,
    val startDate: String,
    val administratorFullName: String,
)
