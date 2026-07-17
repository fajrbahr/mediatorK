package com.fajrbahr.mediatork.sample.university.student.create

import com.fajrbahr.mediatork.api.Request

data class CreateStudentCommand(
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Int>
