package com.fajrbahr.mediatork.sample.university.course.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.course.model.Course

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

data class DeleteCourseCommand(val id: Int) : Request<Unit>

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
