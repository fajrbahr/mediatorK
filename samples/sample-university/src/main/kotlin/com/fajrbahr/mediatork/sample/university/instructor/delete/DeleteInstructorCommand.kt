package com.fajrbahr.mediatork.sample.university.instructor.delete

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class DeleteInstructorQuery(val id: Int? = null) : Request<DeleteInstructorCommand?>

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteInstructorCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val hireDate: String = "",
    val officeLocation: String = "",
) : Request<Unit>
