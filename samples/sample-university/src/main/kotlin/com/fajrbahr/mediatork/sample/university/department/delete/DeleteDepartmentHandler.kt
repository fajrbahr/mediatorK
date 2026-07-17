package com.fajrbahr.mediatork.sample.university.department.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

fun deleteDepartmentHandler(
    store: DepartmentStore,
): Handler<DeleteDepartmentCommand, Unit> = { request ->
    store.delete(request.id)
}
