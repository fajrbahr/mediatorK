package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.department.model.Department
import kotlin.time.Duration.Companion.seconds

data object GetDepartmentsQuery : Request<List<Department>>

fun getDepartments(store: DepartmentStore): Feature<GetDepartmentsQuery, List<Department>> =
    feature {
        handle { store.findAll() }
            .cache(keyFrom = { "all-departments" })
            .measure()
    }

data class GetDepartmentQuery(val id: Int) : Request<Department?>

fun getDepartment(store: DepartmentStore): Feature<GetDepartmentQuery, Department?> =
    feature {
        handle { request -> store.findById(request.id) }
            .cache(keyFrom = { it.id.toString() })
            .timeout(2.seconds)
    }
