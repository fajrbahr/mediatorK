package com.fajrbahr.mediatork.sample.university.instructor.model

import com.fajrbahr.mediatork.sample.university.model.IEntity

data class Instructor(
    override val id: Int,
    val lastName: String,
    val firstMidName: String,
    val hireDate: String,
    val officeLocation: String? = null,
    val courseIds: List<Int> = emptyList(),
) : IEntity {
    val fullName: String get() = "$lastName, $firstMidName"
}
