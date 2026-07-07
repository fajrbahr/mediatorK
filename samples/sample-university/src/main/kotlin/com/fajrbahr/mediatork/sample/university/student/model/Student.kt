package com.fajrbahr.mediatork.sample.university.student.model

data class Student(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val enrollmentDate: String,
) {
    val fullName: String get() = "$lastName, $firstMidName"
}
