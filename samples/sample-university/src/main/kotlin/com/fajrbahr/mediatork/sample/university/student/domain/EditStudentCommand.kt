package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.validator.rules

data class EditStudentCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Unit>

val editStudentValidator: RequestValidator<EditStudentCommand> = validator { request ->
    rules<String> {
        check(request.lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(request.firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(request.enrollmentDate.isNotBlank()) { "Enrollment date is required" }
    }
}

fun editStudent(store: StudentStore): Feature<EditStudentCommand, Unit> =
    feature {
        validate(editStudentValidator)
        handle { request ->
            val existing = store.findById(request.id) ?: return@handle
            store.save(
                existing.copy(
                    lastName = request.lastName,
                    firstMidName = request.firstMidName,
                    enrollmentDate = request.enrollmentDate,
                )
            )
        }
    }
