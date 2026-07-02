package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore

data class DeleteInstructorCommand(val id: Int) : Request<Unit>

class DeleteInstructorHandler(
    private val store: InstructorStore,
    private val departmentStore: DepartmentStore,
) : RequestHandler<DeleteInstructorCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteInstructorCommand,
    ) {
        store.delete(request.id)
        for (dept in departmentStore.findAll()) {
            if (dept.administratorId == request.id) {
                departmentStore.save(dept.copy(administratorId = null))
            }
        }
    }
}
