package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.model.Course
import com.fajrbahr.mediatork.validator.rules

data class GetCourseQuery(val id: Int) : Request<Course?>

class GetCourseHandler(
    private val store: CourseStore,
) : RequestHandler<GetCourseQuery, Course?> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetCourseQuery,
    ): Course? = store.findById(request.id)
}

data class EditCourseCommand(
    val id: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentId: Int = 0,
) : Request<Unit> {
    override fun validate() = rules<String> {
        check(title.length in 3..50) { "Title must be between 3 and 50 characters" }
        check(credits in 0..5) { "Credits must be between 0 and 5" }
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
