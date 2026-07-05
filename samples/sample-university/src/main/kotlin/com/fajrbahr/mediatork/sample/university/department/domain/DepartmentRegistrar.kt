package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

fun departmentRegistrar(store: DepartmentStore): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.apply {
            register(getDepartments(store))
            register(getDepartment(store))
            register(createDepartment(store))
            register(editDepartment(store))
            register(deleteDepartment(store))
        }
    }
}
