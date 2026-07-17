package com.fajrbahr.mediatork.sample.university.student.detail

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.sample.university.model.Grade

// ── Query ───────────────────────────────────────────────────────────────────

data class GetStudentQuery(val id: Int) : Request<StudentDetailModel?>

data class StudentDetailModel(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val enrollmentDate: String,
    val enrollments: List<EnrollmentModel> = emptyList(),
) {
    data class EnrollmentModel(
        val courseId: Int,
        val courseTitle: String,
        val grade: Grade? = null,
    )
}

// ── Enroll ───────────────────────────────────────────────────────────────────

data class EnrollStudentCommand(
    val studentId: Int,
    val courseId: Int,
    val grade: Grade? = null,
) : Request<Int>
