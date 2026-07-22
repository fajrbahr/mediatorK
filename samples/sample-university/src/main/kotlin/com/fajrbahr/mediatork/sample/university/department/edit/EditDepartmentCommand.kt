package com.fajrbahr.mediatork.sample.university.department.edit

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.model.Department
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

// ── Query ───────────────────────────────────────────────────────────────────

data class EditDepartmentQuery(val id: Int? = null) : Request<EditDepartmentCommand?>

class EditDepartmentQueryValidator : RequestValidator<EditDepartmentQuery> {
    override fun validate(request: EditDepartmentQuery): ValidationResult = rules {
        check(request.id != null) { "Id is required" }
    }
}

class EditDepartmentQueryHandler(
    private val store: DepartmentStore,
) : RequestHandler<EditDepartmentQuery, EditDepartmentCommand?> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EditDepartmentQuery,
    ): EditDepartmentCommand? {
        val dept = store.findById(request.id!!) ?: return null
        return EditDepartmentCommand(
            id = dept.id,
            name = dept.name,
            budget = dept.budget,
            startDate = dept.startDate,
            administratorId = dept.administratorId,
        )
    }
}

// ── Command ─────────────────────────────────────────────────────────────────

data class EditDepartmentCommand(
    val id: Int = 0,
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorId: Int? = null,
) : Request<Unit>

class EditDepartmentValidator : RequestValidator<EditDepartmentCommand> {
    override fun validate(request: EditDepartmentCommand): ValidationResult = rules {
        check(request.name.length in 3..50) { "Name must be between 3 and 50 characters" }
        check(request.budget >= 0) { "Budget must be non-negative" }
        check(request.startDate.isNotBlank()) { "Start date is required" }
    }
}

class EditDepartmentHandler(
    private val store: DepartmentStore,
) : RequestHandler<EditDepartmentCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EditDepartmentCommand,
    ) {
        val existing = store.findById(request.id) ?: return
        store.save(
            existing.copy(
                name = request.name,
                budget = request.budget,
                startDate = request.startDate,
                administratorId = request.administratorId,
            )
        )
    }
}
