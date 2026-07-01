package com.fajrbahr.mediatork.sample.university.instructor.model

data class Instructor(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val hireDate: String,
    val officeLocation: String? = null,
    val courseIds: List<Int> = emptyList(),
) {
    val fullName: String get() = "$lastName, $firstMidName"
}
