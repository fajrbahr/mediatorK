package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import kotlin.time.Duration.Companion.seconds

data class DeleteCourseCommand(val id: Int) : Request<Unit>

fun deleteCourse(store: CourseStore): Feature<DeleteCourseCommand, Unit> =
    feature {
        handle { request ->
            store.delete(request.id)
        }
            .timeout(2.seconds)
            .measure()
    }
