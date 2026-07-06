package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.mapper
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.sample.university.student.model.Student
import com.fajrbahr.mediatork.validator.rules
import kotlin.time.Duration.Companion.seconds

data class CreateStudentCommand(
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Int>

val createStudentValidator: RequestValidator<CreateStudentCommand> = validator { request ->
    rules<String> {
        check(request.lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(request.firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(request.enrollmentDate.isNotBlank()) { "Enrollment date is required" }
    }
}

val createStudentMapper = mapper<Student, Int> { student -> student.id }

fun createStudent(store: StudentStore): Feature<CreateStudentCommand, Int> =
    feature<CreateStudentCommand, Student, Int> {
        handle { request ->
            val student = Student(
                id = store.nextId(),
                lastName = request.lastName,
                firstMidName = request.firstMidName,
                enrollmentDate = request.enrollmentDate,
            )
            store.save(student)
            student
        }
            .retry(2)
            .timeout(3.seconds)
            .measure()

        validate(createStudentValidator)
        mapper(createStudentMapper)
    }
