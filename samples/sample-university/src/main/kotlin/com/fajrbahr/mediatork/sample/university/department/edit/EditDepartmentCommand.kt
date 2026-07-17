package com.fajrbahr.mediatork.sample.university.department.edit

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class EditDepartmentQuery(val id: Int? = null) : Request<EditDepartmentCommand?>

// ── Command ─────────────────────────────────────────────────────────────────

data class EditDepartmentCommand(
    val id: Int = 0,
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorId: Int? = null,
) : Request<Unit>
