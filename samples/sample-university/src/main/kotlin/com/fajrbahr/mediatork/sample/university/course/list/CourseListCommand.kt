package com.fajrbahr.mediatork.sample.university.course.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

data object GetCoursesQuery : Request<GetCoursesResult>

data class GetCoursesResult(
    val courses: List<CourseListModel>,
)

data class CourseListModel(
    val id: Int,
    val title: String,
    val credits: Int,
    val departmentName: String,
)

class GetCoursesHandler(
    private val store: CourseStore,
    private val departmentStore: DepartmentStore,
) : RequestHandler<GetCoursesQuery, GetCoursesResult> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetCoursesQuery,
    ): GetCoursesResult {
        val courses = store.findAll().map { course ->
            val department = departmentStore.findById(course.departmentId)
            CourseListModel(
                id = course.id,
                title = course.title,
                credits = course.credits,
                departmentName = department?.name ?: "",
            )
        }
        return GetCoursesResult(courses = courses)
    }
}
