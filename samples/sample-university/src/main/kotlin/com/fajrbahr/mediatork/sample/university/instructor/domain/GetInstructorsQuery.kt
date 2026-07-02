package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor

data object GetInstructorsQuery : Request<List<Instructor>>

fun getInstructors(store: InstructorStore): Feature<GetInstructorsQuery, List<Instructor>> =
    feature {
        handle { store.findAll() }
    }

data class GetInstructorQuery(val id: Int) : Request<Instructor?>

fun getInstructor(store: InstructorStore): Feature<GetInstructorQuery, Instructor?> =
    feature {
        handle { request -> store.findById(request.id) }
    }
