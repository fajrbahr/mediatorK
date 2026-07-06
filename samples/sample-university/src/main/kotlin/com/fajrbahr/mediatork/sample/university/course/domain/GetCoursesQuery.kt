package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.course.model.Course
import kotlin.time.Duration.Companion.seconds

data object GetCoursesQuery : Request<List<Course>>

fun getCourses(store: CourseStore): Feature<GetCoursesQuery, List<Course>> =
    feature {
        handle { store.findAll() }
            .cache(keyFrom = { "all-courses" })
            .measure()
    }
