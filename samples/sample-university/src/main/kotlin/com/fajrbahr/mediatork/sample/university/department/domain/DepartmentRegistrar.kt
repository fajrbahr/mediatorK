package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

class DepartmentRegistrar(private val store: DepartmentStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +GetDepartmentsHandler(store)
            +GetDepartmentHandler(store)
            +CreateDepartmentHandler(store)
            +EditDepartmentHandler(store)
            +DeleteDepartmentHandler(store)
        }
    }
}
