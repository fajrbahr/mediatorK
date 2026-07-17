package com.fajrbahr.mediatork.sample.university.department.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun deleteDepartmentQueryHandler(
    store: DepartmentStore,
    instructorStore: InstructorStore,
): Handler<DeleteDepartmentQuery, DeleteDepartmentCommand?> = handler@{ request ->
    val dept = store.findById(request.id!!) ?: return@handler null
    val administrator = dept.administratorId?.let { instructorStore.findById(it) }
    DeleteDepartmentCommand(
        id = dept.id,
        name = dept.name,
        budget = dept.budget,
        startDate = dept.startDate,
        administratorFullName = administrator?.fullName ?: "",
    )
}
