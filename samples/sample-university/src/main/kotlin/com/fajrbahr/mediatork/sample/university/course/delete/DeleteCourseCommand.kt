package com.fajrbahr.mediatork.sample.university.course.delete

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class DeleteCourseQuery(val id: Int? = null) : Request<DeleteCourseCommand?>

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteCourseCommand(
    val id: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentName: String = "",
) : Request<Unit>
