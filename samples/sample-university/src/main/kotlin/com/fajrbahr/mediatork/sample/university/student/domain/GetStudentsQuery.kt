package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.student.model.Student
import kotlin.time.Duration.Companion.seconds

data object GetStudentsQuery : Request<List<Student>>

fun getStudents(store: StudentStore): Feature<GetStudentsQuery, List<Student>> =
    feature {
        handle { store.findAll() }
            .cache(keyFrom = { "all-students" })
            .measure()
    }

data class GetStudentQuery(val id: Int) : Request<Student?>

fun getStudent(store: StudentStore): Feature<GetStudentQuery, Student?> =
    feature {
        handle { request -> store.findById(request.id) }
            .cache(keyFrom = { it.id.toString() })
            .timeout(2.seconds)
    }

data class GetStudentEnrollmentsQuery(val studentId: Int) : Request<List<Enrollment>>

fun getStudentEnrollments(store: StudentStore): Feature<GetStudentEnrollmentsQuery, List<Enrollment>> =
    feature {
        handle { request ->
            store.findEnrollmentsByStudentId(request.studentId)
        }
            .cache(keyFrom = { it.studentId.toString() })
            .measure()
    }
