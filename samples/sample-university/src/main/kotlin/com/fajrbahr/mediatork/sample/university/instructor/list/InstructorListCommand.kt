package com.fajrbahr.mediatork.sample.university.instructor.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor

data object GetInstructorsQuery : Request<List<Instructor>>

class GetInstructorsHandler(
    private val store: InstructorStore,
) : RequestHandler<GetInstructorsQuery, List<Instructor>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetInstructorsQuery,
    ): List<Instructor> = store.findAll()
}
