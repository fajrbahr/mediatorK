package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.mapper
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.model.Grade
import kotlin.time.Duration.Companion.seconds

data class EnrollStudentCommand(
    val studentId: Int,
    val courseId: Int,
    val grade: Grade? = null,
) : Request<Int>

val enrollStudentMapper = mapper<Enrollment, Int> { it.id }

fun enrollStudent(store: StudentStore): Feature<EnrollStudentCommand, Int> =
    feature<EnrollStudentCommand, Enrollment, Int> {
        handle { request ->
            val enrollment = Enrollment(
                id = store.nextEnrollmentId(),
                courseId = request.courseId,
                studentId = request.studentId,
                grade = request.grade,
            )
            store.saveEnrollment(enrollment)
            enrollment
        }
            .timeout(3.seconds)
            .measure()

        mapper(enrollStudentMapper)
    }
