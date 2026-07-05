package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore

fun instructorRegistrar(
    store: InstructorStore,
    departmentStore: DepartmentStore,
): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.apply {
            register(getInstructors(store))
            register(getInstructor(store))
            register(createEditInstructor(store, departmentStore))
            register(deleteInstructor(store, departmentStore))
        }
    }
}
