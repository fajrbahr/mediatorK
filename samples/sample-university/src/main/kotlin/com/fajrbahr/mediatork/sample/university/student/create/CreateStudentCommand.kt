package com.fajrbahr.mediatork.sample.university.student.create

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.student.model.Student
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

data class CreateStudentCommand(
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Int>

class CreateStudentValidator : RequestValidator<CreateStudentCommand> {
    override fun validate(request: CreateStudentCommand): ValidationResult = rules {
        check(request.lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(request.firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(request.enrollmentDate.isNotBlank()) { "Enrollment date is required" }
    }
}

class CreateStudentHandler(
    private val store: StudentStore,
) : RequestHandler<CreateStudentCommand, Int> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateStudentCommand,
    ): Int {
        val student = Student(
            id = store.nextId(),
            lastName = request.lastName,
            firstMidName = request.firstMidName,
            enrollmentDate = request.enrollmentDate,
        )
        store.save(student)
        return student.id
    }
}
