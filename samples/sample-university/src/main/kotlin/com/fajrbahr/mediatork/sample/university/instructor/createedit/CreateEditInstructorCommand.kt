package com.fajrbahr.mediatork.sample.university.instructor.createedit

import com.fajrbahr.mediatork.api.Request

// ── Query ───────────────────────────────────────────────────────────────────

data class CreateEditInstructorQuery(val id: Int? = null) : Request<CreateEditInstructorCommand>

// ── Command ─────────────────────────────────────────────────────────────────

data class CreateEditInstructorCommand(
    val id: Int? = null,
    val lastName: String = "",
    val firstMidName: String = "",
    val hireDate: String = "",
    val officeLocation: String? = null,
    val selectedCourseIds: List<Int> = emptyList(),
) : Request<Int>
