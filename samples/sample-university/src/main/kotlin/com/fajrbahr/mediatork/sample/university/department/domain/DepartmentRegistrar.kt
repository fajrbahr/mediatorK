package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

class DepartmentRegistrar(private val store: DepartmentStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetDepartmentsHandler(store)
        registry register GetDepartmentHandler(store)
        registry register CreateDepartmentHandler(store)
        registry register EditDepartmentHandler(store)
        registry register DeleteDepartmentHandler(store)
    }
}
