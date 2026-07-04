package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

class StudentRegistrar(private val store: StudentStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.apply {
            +getStudents(store)
            +getStudent(store)
            +getStudentEnrollments(store)
            +createStudent(store)
            +editStudent(store)
            +deleteStudent(store)
            +enrollStudent(store)
        }
    }
}
