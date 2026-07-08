package com.fajrbahr.mediatork.sample.university.about

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

data object AboutQuery : Request<AboutResult>

data class AboutResult(
    val items: List<EnrollmentDateGroup> = emptyList(),
) {
    data class EnrollmentDateGroup(
        val enrollmentDate: String,
        val studentCount: Int,
    )
}

class AboutQueryHandler(
    private val store: StudentStore,
) : RequestHandler<AboutQuery, AboutResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: AboutQuery,
    ): AboutResult {
        val students = store.findAll()
        val groups = students
            .groupBy { it.enrollmentDate }
            .map { (date, list) ->
                AboutResult.EnrollmentDateGroup(
                    enrollmentDate = date,
                    studentCount = list.size,
                )
            }
            .sortedBy { it.enrollmentDate }
        return AboutResult(items = groups)
    }
}
