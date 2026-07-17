package com.fajrbahr.mediatork.sample.university.student.list

import com.fajrbahr.mediatork.api.Request

data object GetStudentsQuery : Request<List<StudentListModel>>

data class StudentListModel(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val enrollmentDate: String,
    val enrollmentsCount: Int,
)
