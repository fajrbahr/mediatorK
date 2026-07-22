package com.fajrbahr.mediatork.sample.university.course.edit

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.course.model.Course
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

// ── Query ───────────────────────────────────────────────────────────────────

data class EditCourseQuery(val id: Int? = null) : Request<EditCourseCommand?>

class EditCourseQueryValidator : RequestValidator<EditCourseQuery> {
    override fun validate(request: EditCourseQuery): ValidationResult = rules {
        check(request.id != null) { "Id is required" }
    }
}

class EditCourseQueryHandler(
    private val store: CourseStore,
) : RequestHandler<EditCourseQuery, EditCourseCommand?> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EditCourseQuery,
    ): EditCourseCommand? {
        val course = store.findById(request.id!!) ?: return null
        return EditCourseCommand(
            id = course.id,
            number = course.number,
            title = course.title,
            credits = course.credits,
            departmentId = course.departmentId,
        )
    }
}

// ── Command ─────────────────────────────────────────────────────────────────

data class EditCourseCommand(
    val id: Int = 0,
    val number: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentId: Int = 0,
) : Request<Unit>

class EditCourseValidator : RequestValidator<EditCourseCommand> {
    override fun validate(request: EditCourseCommand): ValidationResult = rules {
        check(request.title.length in 3..50) { "Title must be between 3 and 50 characters" }
        check(request.credits in 0..5) { "Credits must be between 0 and 5" }
    }
}

class EditCourseHandler(
    private val store: CourseStore,
) : RequestHandler<EditCourseCommand, Unit> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EditCourseCommand,
    ) {
        val existing = store.findById(request.id) ?: return
        store.save(
            existing.copy(
                title = request.title,
                credits = request.credits,
                departmentId = request.departmentId,
            )
        )
    }
}
