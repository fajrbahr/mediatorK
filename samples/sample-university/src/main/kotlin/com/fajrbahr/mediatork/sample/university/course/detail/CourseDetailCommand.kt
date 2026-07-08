package com.fajrbahr.mediatork.sample.university.course.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

// ── Query ───────────────────────────────────────────────────────────────────

data class GetCourseQuery(val id: Int) : Request<CourseDetailModel?>

data class CourseDetailModel(
    val id: Int,
    val number: Int,
    val title: String,
    val credits: Int,
    val departmentName: String,
)

class GetCourseHandler(
    private val store: CourseStore,
    private val departmentStore: DepartmentStore,
) : RequestHandler<GetCourseQuery, CourseDetailModel?> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetCourseQuery,
    ): CourseDetailModel? {
        val course = store.findById(request.id) ?: return null
        val department = departmentStore.findById(course.departmentId)
        return CourseDetailModel(
            id = course.id,
            number = course.number,
            title = course.title,
            credits = course.credits,
            departmentName = department?.name ?: "",
        )
    }
}

// ── Delete ──────────────────────────────────────────────────────────────────

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
