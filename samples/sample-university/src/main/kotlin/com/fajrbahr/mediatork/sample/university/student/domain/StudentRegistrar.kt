package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentHandler
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentValidator

class StudentRegistrar(private val store: StudentStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetStudentsHandler(store)
        registry register GetStudentHandler(store)
        registry register GetStudentEnrollmentsHandler(store)
        registry register CreateStudentHandler(store)
        registry.registerValidator(CreateStudentValidator())
        registry register EditStudentHandler(store)
        registry.registerValidator(EditStudentValidator())
        registry register DeleteStudentHandler(store)
        registry register EnrollStudentHandler(store)
    }
}
