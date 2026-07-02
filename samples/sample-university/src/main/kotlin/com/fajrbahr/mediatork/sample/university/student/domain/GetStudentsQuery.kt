package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.student.model.Student

data object GetStudentsQuery : Request<List<Student>>

fun getStudents(store: StudentStore): Feature<GetStudentsQuery, List<Student>> =
    feature {
        handle { store.findAll() }
    }

data class GetStudentQuery(val id: Int) : Request<Student?>

fun getStudent(store: StudentStore): Feature<GetStudentQuery, Student?> =
    feature {
        handle { request -> store.findById(request.id) }
    }

data class GetStudentEnrollmentsQuery(val studentId: Int) : Request<List<Enrollment>>

fun getStudentEnrollments(store: StudentStore): Feature<GetStudentEnrollmentsQuery, List<Enrollment>> =
    feature {
        handle { request -> store.findEnrollmentsByStudentId(request.studentId) }
    }
