package com.fajrbahr.mediatork.sample.university.instructor.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun deleteInstructorHandler(
    store: InstructorStore,
    departmentStore: DepartmentStore,
): Handler<DeleteInstructorCommand, Unit> = { request ->
    store.delete(request.id)
    for (dept in departmentStore.findAll()) {
        if (dept.administratorId == request.id) {
            departmentStore.save(dept.copy(administratorId = null))
        }
    }
}
