package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor

class InstructorStore {
    private val instructors = mutableMapOf<Int, Instructor>()
    private var idSeq = 0

    fun nextId(): Int = ++idSeq
    fun save(instructor: Instructor) {
        instructors[instructor.id] = instructor
    }

    fun findById(id: Int): Instructor? = instructors[id]
    fun findAll(): List<Instructor> = instructors.values.sortedBy { it.lastName }
    fun delete(id: Int) {
        instructors.remove(id)
    }
}
