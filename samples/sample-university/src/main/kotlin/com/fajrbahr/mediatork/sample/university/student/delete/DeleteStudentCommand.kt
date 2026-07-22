package com.fajrbahr.mediatork.sample.university.student.delete

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

// ── Query ───────────────────────────────────────────────────────────────────

data class DeleteStudentQuery(val id: Int? = null) : Request<DeleteStudentCommand?>

class DeleteStudentQueryValidator : RequestValidator<DeleteStudentQuery> {
    override fun validate(request: DeleteStudentQuery): ValidationResult = rules {
        check(request.id != null) { "Id is required" }
    }
}

class DeleteStudentQueryHandler(
    private val store: StudentStore,
) : RequestHandler<DeleteStudentQuery, DeleteStudentCommand?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteStudentQuery,
    ): DeleteStudentCommand? {
        val student = store.findById(request.id!!) ?: return null
        return DeleteStudentCommand(
            id = student.id,
            lastName = student.lastName,
            firstMidName = student.firstMidName,
            enrollmentDate = student.enrollmentDate,
        )
    }
}

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteStudentCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Unit>

class DeleteStudentHandler(
    private val store: StudentStore,
) : RequestHandler<DeleteStudentCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteStudentCommand,
    ) {
        store.delete(request.id)
    }
}
