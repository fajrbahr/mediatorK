package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.sample.university.department.model.Department

class DepartmentStore {
    private val departments = mutableMapOf<Int, Department>()
    private var idSeq = 0

    fun nextId(): Int = ++idSeq
    fun save(department: Department) {
        departments[department.id] = department
    }

    fun findById(id: Int): Department? = departments[id]
    fun findAll(): List<Department> = departments.values.sortedBy { it.id }
    fun delete(id: Int) {
        departments.remove(id)
    }
}
