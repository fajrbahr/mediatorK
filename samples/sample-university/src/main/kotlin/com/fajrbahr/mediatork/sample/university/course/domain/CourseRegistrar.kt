package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

fun courseRegistrar(store: CourseStore): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.apply {
            register(getCourses(store))
            register(getCourse(store))
            register(createCourse(store))
            register(editCourse(store))
            register(deleteCourse(store))
        }
    }
}
