package com.fajrbahr.mediatork.sample.university.course

import android.content.SharedPreferences
import com.fajrbahr.mediatork.sample.university.course.model.Course
import org.json.JSONArray
import org.json.JSONObject

class CourseStore(private val prefs: SharedPreferences) {

    fun nextId(): Int {
        val id = prefs.getInt("course_id_seq", 0) + 1
        prefs.edit().putInt("course_id_seq", id).apply()
        return id
    }

    fun save(course: Course) {
        val courses = loadMap()
        courses[course.id] = course
        persist(courses)
    }

    fun findById(id: Int): Course? = loadMap()[id]

    fun findAll(): List<Course> = loadMap().values.sortedBy { it.id }

    fun delete(id: Int) {
        val courses = loadMap()
        courses.remove(id)
        persist(courses)
    }

    private fun loadMap(): MutableMap<Int, Course> {
        val json = prefs.getString("courses", null) ?: return mutableMapOf()
        val array = JSONArray(json)
        val map = mutableMapOf<Int, Course>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val course = Course(
                id = obj.getInt("id"),
                number = obj.getInt("number"),
                title = obj.getString("title"),
                credits = obj.getInt("credits"),
                departmentId = obj.getInt("departmentId"),
            )
            map[course.id] = course
        }
        return map
    }

    private fun persist(courses: Map<Int, Course>) {
        val array = JSONArray()
        for (course in courses.values) {
            val obj = JSONObject()
            obj.put("id", course.id)
            obj.put("number", course.number)
            obj.put("title", course.title)
            obj.put("credits", course.credits)
            obj.put("departmentId", course.departmentId)
            array.put(obj)
        }
        prefs.edit().putString("courses", array.toString()).apply()
    }
}
