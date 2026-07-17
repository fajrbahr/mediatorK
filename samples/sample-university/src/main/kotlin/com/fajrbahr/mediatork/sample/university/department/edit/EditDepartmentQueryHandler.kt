package com.fajrbahr.mediatork.sample.university.department.edit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

fun editDepartmentQueryHandler(
    store: DepartmentStore,
): Handler<EditDepartmentQuery, EditDepartmentCommand?> = handler@{ request ->
    val dept = store.findById(request.id!!) ?: return@handler null
    EditDepartmentCommand(
        id = dept.id,
        name = dept.name,
        budget = dept.budget,
        startDate = dept.startDate,
        administratorId = dept.administratorId,
    )
}
