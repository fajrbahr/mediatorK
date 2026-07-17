package com.fajrbahr.mediatork.sample.university.course.edit

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class EditCourseQuery(val id: Int? = null) : Request<EditCourseCommand?>

// ── Command ─────────────────────────────────────────────────────────────────

data class EditCourseCommand(
    val id: Int = 0,
    val number: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentId: Int = 0,
) : Request<Unit>
