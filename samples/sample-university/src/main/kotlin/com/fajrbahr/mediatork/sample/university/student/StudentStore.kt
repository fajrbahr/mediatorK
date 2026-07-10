package com.fajrbahr.mediatork.sample.university.student

import android.content.SharedPreferences
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.model.Student
import org.json.JSONArray
import org.json.JSONObject

class StudentStore(private val prefs: SharedPreferences) {

    fun nextId(): Int {
        val id = prefs.getInt("student_id_seq", 0) + 1
        prefs.edit().putInt("student_id_seq", id).apply()
        return id
    }

    fun save(student: Student) {
        val students = loadStudentsMap()
        students[student.id] = student
        saveStudentsMap(students)
    }

    fun findById(id: Int): Student? = loadStudentsMap()[id]

    fun findAll(): List<Student> = loadStudentsMap().values.sortedBy { it.lastName }

    fun delete(id: Int) {
        val students = loadStudentsMap()
        students.remove(id)
        saveStudentsMap(students)
    }

    fun nextEnrollmentId(): Int {
        val id = prefs.getInt("enrollment_id_seq", 0) + 1
        prefs.edit().putInt("enrollment_id_seq", id).apply()
        return id
    }

    fun saveEnrollment(enrollment: Enrollment) {
        val enrollments = loadEnrollments().toMutableList()
        enrollments.add(enrollment)
        saveEnrollments(enrollments)
    }

    fun findEnrollmentsByStudentId(studentId: Int): List<Enrollment> =
        loadEnrollments().filter { it.studentId == studentId }

    fun findEnrollmentsByCourseId(courseId: Int): List<Enrollment> =
        loadEnrollments().filter { it.courseId == courseId }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun loadStudentsMap(): MutableMap<Int, Student> {
        val json = prefs.getString("students", null) ?: return mutableMapOf()
        val array = JSONArray(json)
        val map = mutableMapOf<Int, Student>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val student = Student(
                id = obj.getInt("id"),
                lastName = obj.getString("lastName"),
                firstMidName = obj.getString("firstMidName"),
                enrollmentDate = obj.getString("enrollmentDate"),
            )
            map[student.id] = student
        }
        return map
    }

    private fun saveStudentsMap(students: Map<Int, Student>) {
        val array = JSONArray()
        students.values.forEach { student ->
            val obj = JSONObject().apply {
                put("id", student.id)
                put("lastName", student.lastName)
                put("firstMidName", student.firstMidName)
                put("enrollmentDate", student.enrollmentDate)
            }
            array.put(obj)
        }
        prefs.edit().putString("students", array.toString()).apply()
    }

    private fun loadEnrollments(): List<Enrollment> {
        val json = prefs.getString("enrollments", null) ?: return emptyList()
        val array = JSONArray(json)
        val list = mutableListOf<Enrollment>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val enrollment = Enrollment(
                id = obj.getInt("id"),
                courseId = obj.getInt("courseId"),
                studentId = obj.getInt("studentId"),
                grade = if (obj.isNull("grade")) null else Grade.valueOf(obj.getString("grade")),
            )
            list.add(enrollment)
        }
        return list
    }

    private fun saveEnrollments(enrollments: List<Enrollment>) {
        val array = JSONArray()
        enrollments.forEach { enrollment ->
            val obj = JSONObject().apply {
                put("id", enrollment.id)
                put("courseId", enrollment.courseId)
                put("studentId", enrollment.studentId)
                put("grade", enrollment.grade?.name ?: JSONObject.NULL)
            }
            array.put(obj)
        }
        prefs.edit().putString("enrollments", array.toString()).apply()
    }
}
