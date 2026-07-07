package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

class CourseRegistrar(private val store: CourseStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetCoursesHandler(store)
        registry register GetCourseHandler(store)
        registry register CreateCourseHandler(store)
        registry.registerValidator(CreateCourseValidator())
        registry register EditCourseHandler(store)
        registry.registerValidator(EditCourseValidator())
        registry register DeleteCourseHandler(store)
    }
}
