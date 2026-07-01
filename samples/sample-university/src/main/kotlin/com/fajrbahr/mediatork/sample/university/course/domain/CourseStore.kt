package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.sample.university.course.model.Course

class CourseStore {
    private val courses = mutableMapOf<Int, Course>()
    private var idSeq = 0

    fun nextId(): Int = ++idSeq
    fun save(course: Course) {
        courses[course.id] = course
    }

    fun findById(id: Int): Course? = courses[id]
    fun findAll(): List<Course> = courses.values.sortedBy { it.id }
    fun delete(id: Int) {
        courses.remove(id)
    }
}
