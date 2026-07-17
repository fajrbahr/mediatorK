package com.fajrbahr.mediatork.sample.university.department.edit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

fun editDepartmentHandler(
    store: DepartmentStore,
): Handler<EditDepartmentCommand, Unit> = handler@{ request ->
    val existing = store.findById(request.id) ?: return@handler
    store.save(
        existing.copy(
            name = request.name,
            budget = request.budget,
            startDate = request.startDate,
            administratorId = request.administratorId,
        )
    )
}
