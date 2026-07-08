package com.fajrbahr.mediatork.sample.university.course.delete

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

// ── Query ───────────────────────────────────────────────────────────────────

data class DeleteCourseQuery(val id: Int? = null) : Request<DeleteCourseCommand?>

class DeleteCourseQueryValidator : RequestValidator<DeleteCourseQuery> {
    override fun validate(request: DeleteCourseQuery): ValidationResult = rules {
        check(request.id != null) { "Id is required" }
    }
}

class DeleteCourseQueryHandler(
    private val store: CourseStore,
    private val departmentStore: DepartmentStore,
) : RequestHandler<DeleteCourseQuery, DeleteCourseCommand?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteCourseQuery,
    ): DeleteCourseCommand? {
        val course = store.findById(request.id!!) ?: return null
        val department = departmentStore.findById(course.departmentId)
        return DeleteCourseCommand(
            id = course.id,
            title = course.title,
            credits = course.credits,
            departmentName = department?.name ?: "",
        )
    }
}

// ── Command ─────────────────────────────────────────────────────────────────

data class DeleteCourseCommand(
    val id: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentName: String = "",
) : Request<Unit>

class DeleteCourseHandler(
    private val store: CourseStore,
) : RequestHandler<DeleteCourseCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteCourseCommand,
    ) {
        store.delete(request.id)
    }
}
