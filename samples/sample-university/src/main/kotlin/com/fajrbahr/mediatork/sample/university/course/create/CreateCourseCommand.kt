package com.fajrbahr.mediatork.sample.university.course.create

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.course.model.Course
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

data class CreateCourseCommand(
    val number: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentId: Int = 0,
) : Request<Int>

class CreateCourseValidator : RequestValidator<CreateCourseCommand> {
    override fun validate(request: CreateCourseCommand): ValidationResult = rules {
        check(request.number > 0) { "Number must be greater than 0" }
        check(request.title.length in 3..50) { "Title must be between 3 and 50 characters" }
        check(request.credits in 0..5) { "Credits must be between 0 and 5" }
    }
}

class CreateCourseHandler(
    private val store: CourseStore,
) : RequestHandler<CreateCourseCommand, Int> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateCourseCommand,
    ): Int {
        val course = Course(
            id = store.nextId(),
            number = request.number,
            title = request.title,
            credits = request.credits,
            departmentId = request.departmentId,
        )
        store.save(course)
        return course.id
    }
}
