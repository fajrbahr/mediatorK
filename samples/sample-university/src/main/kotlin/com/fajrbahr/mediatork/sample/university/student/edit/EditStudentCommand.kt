package com.fajrbahr.mediatork.sample.university.student.edit

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class EditStudentQuery(val id: Int? = null) : Request<EditStudentCommand?>

// ── Command ─────────────────────────────────────────────────────────────────

data class EditStudentCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Unit>
