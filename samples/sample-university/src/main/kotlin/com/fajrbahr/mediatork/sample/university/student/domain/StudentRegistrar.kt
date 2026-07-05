package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

fun studentRegistrar(store: StudentStore): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.apply {
            register(getStudents(store))
            register(getStudent(store))
            register(getStudentEnrollments(store))
            register(createStudent(store))
            register(editStudent(store))
            register(deleteStudent(store))
            register(enrollStudent(store))
        }
    }
}
