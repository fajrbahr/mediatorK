package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.mappedFeature
import com.fajrbahr.mediatork.feature.mapper
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.model.Grade

data class EnrollStudentCommand(
    val studentId: Int,
    val courseId: Int,
    val grade: Grade? = null,
) : Request<Int>

val enrollStudentMapper = mapper<Enrollment, Int> { it.id }

fun enrollStudent(store: StudentStore): Feature<EnrollStudentCommand, Int> =
    mappedFeature<EnrollStudentCommand, Int>(enrollStudentMapper) {
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
    }
