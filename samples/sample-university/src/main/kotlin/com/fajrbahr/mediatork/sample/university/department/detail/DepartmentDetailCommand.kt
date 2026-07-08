package com.fajrbahr.mediatork.sample.university.department.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.model.Department

// ── Queries ──────────────────────────────────────────────────────────────────

data class GetDepartmentQuery(val id: Int) : Request<Department?>

class GetDepartmentHandler(
    private val store: DepartmentStore,
) : RequestHandler<GetDepartmentQuery, Department?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetDepartmentQuery,
    ): Department? = store.findById(request.id)
}

// ── Delete ───────────────────────────────────────────────────────────────────

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
