package com.fajrbahr.mediatork.sample.university.student.edit

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.student.model.Student
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

// ── Query ───────────────────────────────────────────────────────────────────

data class EditStudentQuery(val id: Int? = null) : Request<EditStudentCommand?>

class EditStudentQueryValidator : RequestValidator<EditStudentQuery> {
    override fun validate(request: EditStudentQuery): ValidationResult = rules {
        check(request.id != null) { "Id is required" }
    }
}

class EditStudentQueryHandler(
    private val store: StudentStore,
) : RequestHandler<EditStudentQuery, EditStudentCommand?> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EditStudentQuery,
    ): EditStudentCommand? {
        val student = store.findById(request.id!!) ?: return null
        return EditStudentCommand(
            id = student.id,
            lastName = student.lastName,
            firstMidName = student.firstMidName,
            enrollmentDate = student.enrollmentDate,
        )
    }
}

// ── Command ─────────────────────────────────────────────────────────────────

data class EditStudentCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Unit>

class EditStudentValidator : RequestValidator<EditStudentCommand> {
    override fun validate(request: EditStudentCommand): ValidationResult = rules {
        check(request.lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(request.firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(request.enrollmentDate.isNotBlank()) { "Enrollment date is required" }
    }
}

class EditStudentHandler(
    private val store: StudentStore,
) : RequestHandler<EditStudentCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EditStudentCommand,
    ) {
        val existing = store.findById(request.id) ?: return
        store.save(
            existing.copy(
                lastName = request.lastName,
                firstMidName = request.firstMidName,
                enrollmentDate = request.enrollmentDate,
            )
        )
    }
}
