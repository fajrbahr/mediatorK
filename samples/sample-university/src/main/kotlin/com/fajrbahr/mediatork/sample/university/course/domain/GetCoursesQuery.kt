package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.model.Course

data object GetCoursesQuery : Request<List<Course>>

class GetCoursesHandler(
    private val store: CourseStore,
) : RequestHandler<GetCoursesQuery, List<Course>> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetCoursesQuery,
    ): List<Course> = store.findAll()
}
