package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.department.model.Department
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

// ── Queries ──────────────────────────────────────────────────────────────────

data object GetDepartmentsQuery : Request<List<Department>>

class GetDepartmentsHandler(
    private val store: DepartmentStore,
) : RequestHandler<GetDepartmentsQuery, List<Department>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetDepartmentsQuery,
    ): List<Department> = store.findAll()
}

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

// ── Create ───────────────────────────────────────────────────────────────────

data class CreateDepartmentCommand(
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorId: Int? = null,
) : Request<Int>

class CreateDepartmentValidator : RequestValidator<CreateDepartmentCommand> {
    override fun validate(request: CreateDepartmentCommand): ValidationResult = rules {
        check(request.name.length in 3..50) { "Name must be between 3 and 50 characters" }
        check(request.budget >= 0) { "Budget must be non-negative" }
        check(request.startDate.isNotBlank()) { "Start date is required" }
    }
}

class CreateDepartmentHandler(
    private val store: DepartmentStore,
) : RequestHandler<CreateDepartmentCommand, Int> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateDepartmentCommand,
    ): Int {
        val dept = Department(
            id = store.nextId(),
            name = request.name,
            budget = request.budget,
            startDate = request.startDate,
            administratorId = request.administratorId,
        )
        store.save(dept)
        return dept.id
    }
}

// ── Edit ─────────────────────────────────────────────────────────────────────

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
