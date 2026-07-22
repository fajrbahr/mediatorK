package com.fajrbahr.mediatork.sample.university.instructor.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

// ── Query ───────────────────────────────────────────────────────────────────

data class GetInstructorQuery(val id: Int) : Request<InstructorDetailModel?>

data class InstructorDetailModel(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val hireDate: String,
    val officeLocation: String?,
    val courses: List<CourseModel> = emptyList(),
) {
    data class CourseModel(
        val id: Int,
        val title: String,
    )
}

class GetInstructorHandler(
    private val store: InstructorStore,
    private val courseStore: CourseStore,
) : RequestHandler<GetInstructorQuery, InstructorDetailModel?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetInstructorQuery,
    ): InstructorDetailModel? {
        val instructor = store.findById(request.id) ?: return null
        return InstructorDetailModel(
            id = instructor.id,
            lastName = instructor.lastName,
            firstMidName = instructor.firstMidName,
            hireDate = instructor.hireDate,
            officeLocation = instructor.officeLocation,
            courses = instructor.courseIds.mapNotNull { courseId ->
                val course = courseStore.findById(courseId) ?: return@mapNotNull null
                InstructorDetailModel.CourseModel(id = course.id, title = course.title)
            },
        )
    }
}
