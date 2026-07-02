package com.fajrbahr.mediatork.sample.university.student.model

import com.fajrbahr.mediatork.sample.university.model.IEntity

data class Student(
    override val id: Int,
    val lastName: String,
    val firstMidName: String,
    val enrollmentDate: String,
) : IEntity {
    val fullName: String get() = "$lastName, $firstMidName"
}
