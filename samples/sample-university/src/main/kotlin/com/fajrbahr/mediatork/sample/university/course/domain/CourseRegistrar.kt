package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

class CourseRegistrar(private val store: CourseStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +getCourses(store)
            +getCourse(store)
            +createCourse(store)
            +editCourse(store)
            +deleteCourse(store)
        }
    }
}
