package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler

data class DeleteDepartmentCommand(val id: Int) : Request<Unit>

class DeleteDepartmentHandler(
    private val store: DepartmentStore,
) : RequestHandler<DeleteDepartmentCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteDepartmentCommand,
    ) {
        store.delete(request.id)
    }
}
