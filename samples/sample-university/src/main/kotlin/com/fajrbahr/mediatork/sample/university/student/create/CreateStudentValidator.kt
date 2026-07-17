package com.fajrbahr.mediatork.sample.university.student.create

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val createStudentValidator: Validator<CreateStudentCommand> = { request ->
    rules {
        check(request.lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(request.firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(request.enrollmentDate.isNotBlank()) { "Enrollment date is required" }
    }
}
