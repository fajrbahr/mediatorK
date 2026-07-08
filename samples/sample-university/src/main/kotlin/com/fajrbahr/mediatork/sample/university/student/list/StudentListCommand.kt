package com.fajrbahr.mediatork.sample.university.student.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.student.model.Student

data object GetStudentsQuery : Request<List<Student>>

class GetStudentsHandler(
    private val store: StudentStore,
) : RequestHandler<GetStudentsQuery, List<Student>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetStudentsQuery,
    ): List<Student> = store.findAll()
}
