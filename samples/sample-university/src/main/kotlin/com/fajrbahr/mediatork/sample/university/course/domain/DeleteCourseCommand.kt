package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler

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
