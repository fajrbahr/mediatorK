package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import kotlin.time.Duration.Companion.seconds

data object GetInstructorsQuery : Request<List<Instructor>>

fun getInstructors(store: InstructorStore): Feature<GetInstructorsQuery, List<Instructor>> =
    feature {
        handle { store.findAll() }
            .cache(keyFrom = { "all-instructors" })
            .measure()
    }

data class GetInstructorQuery(val id: Int) : Request<Instructor?>

fun getInstructor(store: InstructorStore): Feature<GetInstructorQuery, Instructor?> =
    feature {
        handle { request -> store.findById(request.id) }
            .cache(keyFrom = { it.id.toString() })
            .timeout(2.seconds)
    }
