package com.fajrbahr.mediatork.sample.university.about

import com.fajrbahr.mediatork.api.Request

data object AboutQuery : Request<AboutResult>

data class AboutResult(
    val items: List<EnrollmentDateGroup> = emptyList(),
) {
    data class EnrollmentDateGroup(
        val enrollmentDate: String,
        val studentCount: Int,
    )
}
