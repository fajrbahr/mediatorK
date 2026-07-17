package com.fajrbahr.mediatork.sample.university.department.create

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.model.Department

fun createDepartmentHandler(
    store: DepartmentStore,
): Handler<CreateDepartmentCommand, Int> = { request ->
    val dept = Department(
        id = store.nextId(),
        name = request.name,
        budget = request.budget,
        startDate = request.startDate,
        administratorId = request.administratorId,
    )
    store.save(dept)
    dept.id
}
