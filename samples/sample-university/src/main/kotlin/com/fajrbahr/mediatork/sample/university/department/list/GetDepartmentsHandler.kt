package com.fajrbahr.mediatork.sample.university.department.list

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun getDepartmentsHandler(
    store: DepartmentStore,
    instructorStore: InstructorStore,
): Handler<GetDepartmentsQuery, List<DepartmentListModel>> = { request ->
    store.findAll().map { dept ->
        val administrator = dept.administratorId?.let { instructorStore.findById(it) }
        DepartmentListModel(
            id = dept.id,
            name = dept.name,
            budget = dept.budget,
            startDate = dept.startDate,
            administratorFullName = administrator?.fullName ?: "",
        )
    }
}
