package com.fajrbahr.mediatork.sample.university.instructor.delete

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

data class DeleteInstructorQuery(val id: Int? = null) : Request<DeleteInstructorCommand?>

class DeleteInstructorQueryValidator : RequestValidator<DeleteInstructorQuery> {
    override fun validate(request: DeleteInstructorQuery): ValidationResult = rules {
        check(request.id != null) { "Id is required" }
    }
}

class DeleteInstructorQueryHandler(
    private val store: InstructorStore,
) : RequestHandler<DeleteInstructorQuery, DeleteInstructorCommand?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteInstructorQuery,
    ): DeleteInstructorCommand? {
        val instructor = store.findById(request.id!!) ?: return null
        return DeleteInstructorCommand(
            id = instructor.id,
            lastName = instructor.lastName,
            firstMidName = instructor.firstMidName,
            hireDate = instructor.hireDate,
            officeLocation = instructor.officeLocation ?: "",
        )
    }
}

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteInstructorCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val hireDate: String = "",
    val officeLocation: String = "",
) : Request<Unit>

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
