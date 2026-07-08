package com.fajrbahr.mediatork.sample.university.department.delete

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

// ── Query ───────────────────────────────────────────────────────────────────

data class DeleteDepartmentQuery(val id: Int? = null) : Request<DeleteDepartmentCommand?>

class DeleteDepartmentQueryValidator : RequestValidator<DeleteDepartmentQuery> {
    override fun validate(request: DeleteDepartmentQuery): ValidationResult = rules {
        check(request.id != null) { "Id is required" }
    }
}

class DeleteDepartmentQueryHandler(
    private val store: DepartmentStore,
    private val instructorStore: InstructorStore,
) : RequestHandler<DeleteDepartmentQuery, DeleteDepartmentCommand?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteDepartmentQuery,
    ): DeleteDepartmentCommand? {
        val dept = store.findById(request.id!!) ?: return null
        val administrator = dept.administratorId?.let { instructorStore.findById(it) }
        return DeleteDepartmentCommand(
            id = dept.id,
            name = dept.name,
            budget = dept.budget,
            startDate = dept.startDate,
            administratorFullName = administrator?.fullName ?: "",
        )
    }
}

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteDepartmentCommand(
    val id: Int = 0,
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorFullName: String = "",
) : Request<Unit>

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
