package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.student.model.Student

class StudentStore {
    private val students = mutableMapOf<Int, Student>()
    private val enrollments = mutableListOf<Enrollment>()
    private var idSeq = 0
    private var enrollmentIdSeq = 0

    fun nextId(): Int = ++idSeq
    fun save(student: Student) {
        students[student.id] = student
    }

    fun findById(id: Int): Student? = students[id]
    fun findAll(): List<Student> = students.values.sortedBy { it.lastName }
    fun delete(id: Int) {
        students.remove(id)
    }

    fun nextEnrollmentId(): Int = ++enrollmentIdSeq
    fun saveEnrollment(enrollment: Enrollment) {
        enrollments.add(enrollment)
    }

    fun findEnrollmentsByStudentId(studentId: Int): List<Enrollment> =
        enrollments.filter { it.studentId == studentId }
}
