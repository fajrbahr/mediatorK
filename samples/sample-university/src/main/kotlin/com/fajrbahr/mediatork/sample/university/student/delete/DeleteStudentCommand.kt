package com.fajrbahr.mediatork.sample.university.student.delete

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class DeleteStudentQuery(val id: Int? = null) : Request<DeleteStudentCommand?>

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteStudentCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Unit>
