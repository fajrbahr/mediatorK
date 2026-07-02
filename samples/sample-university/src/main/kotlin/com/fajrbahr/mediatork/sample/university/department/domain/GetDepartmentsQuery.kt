package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.department.model.Department

data object GetDepartmentsQuery : Request<List<Department>>

fun getDepartments(store: DepartmentStore): Feature<GetDepartmentsQuery, List<Department>> =
    feature {
        handle { store.findAll() }
    }

data class GetDepartmentQuery(val id: Int) : Request<Department?>

fun getDepartment(store: DepartmentStore): Feature<GetDepartmentQuery, Department?> =
    feature {
        handle { request -> store.findById(request.id) }
    }
