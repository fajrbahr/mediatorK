package com.fajrbahr.mediatork.sample.university.instructor.list

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.sample.university.model.Grade

data class GetInstructorsQuery(
    val selectedInstructorId: Int? = null,
    val selectedCourseId: Int? = null,
) : Request<InstructorIndexModel>

data class InstructorIndexModel(
    val instructors: List<InstructorRow>,
    val courses: List<CourseRow> = emptyList(),
    val enrollments: List<EnrollmentRow> = emptyList(),
    val selectedInstructorId: Int? = null,
    val selectedCourseId: Int? = null,
) {
    data class InstructorRow(
        val id: Int,
        val lastName: String,
        val firstMidName: String,
        val hireDate: String,
        val officeLocation: String?,
    )

    data class CourseRow(
        val id: Int,
        val number: Int,
        val title: String,
        val departmentName: String,
    )

    data class EnrollmentRow(
        val studentFullName: String,
        val grade: Grade?,
    )
}
