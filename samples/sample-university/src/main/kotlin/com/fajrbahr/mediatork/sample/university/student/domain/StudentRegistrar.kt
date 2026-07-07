package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

class StudentRegistrar(private val store: StudentStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +GetStudentsHandler(store)
            +GetStudentHandler(store)
            +GetStudentEnrollmentsHandler(store)
            +CreateStudentHandler(store)
            +EditStudentHandler(store)
            +DeleteStudentHandler(store)
            +EnrollStudentHandler(store)
        }
    }
}
