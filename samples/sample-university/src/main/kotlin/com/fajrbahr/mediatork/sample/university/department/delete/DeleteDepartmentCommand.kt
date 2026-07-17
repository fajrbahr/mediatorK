package com.fajrbahr.mediatork.sample.university.department.delete

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class DeleteDepartmentQuery(val id: Int? = null) : Request<DeleteDepartmentCommand?>

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteDepartmentCommand(
    val id: Int = 0,
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorFullName: String = "",
) : Request<Unit>
