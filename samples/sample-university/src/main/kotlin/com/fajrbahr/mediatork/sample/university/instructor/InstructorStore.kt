package com.fajrbahr.mediatork.sample.university.instructor

import android.content.SharedPreferences
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import org.json.JSONArray
import org.json.JSONObject

class InstructorStore(private val prefs: SharedPreferences) {

    fun nextId(): Int {
        val id = prefs.getInt("instructor_id_seq", 0) + 1
        prefs.edit().putInt("instructor_id_seq", id).apply()
        return id
    }

    fun save(instructor: Instructor) {
        val all = loadMap()
        all[instructor.id] = instructor
        persist(all)
    }

    fun findById(id: Int): Instructor? = loadMap()[id]

    fun findAll(): List<Instructor> = loadMap().values.sortedBy { it.lastName }

    fun delete(id: Int) {
        val all = loadMap()
        all.remove(id)
        persist(all)
    }

    private fun loadMap(): MutableMap<Int, Instructor> {
        val json = prefs.getString("instructors", null) ?: return mutableMapOf()
        val array = JSONArray(json)
        val map = mutableMapOf<Int, Instructor>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val courseIds = mutableListOf<Int>()
            if (o.has("courseIds")) {
                val arr = o.getJSONArray("courseIds")
                for (j in 0 until arr.length()) {
                    courseIds.add(arr.getInt(j))
                }
            }
            val instructor = Instructor(
                id = o.getInt("id"),
                lastName = o.getString("lastName"),
                firstMidName = o.getString("firstMidName"),
                hireDate = o.getString("hireDate"),
                officeLocation = if (o.has("officeLocation") && !o.isNull("officeLocation")) o.getString("officeLocation") else null,
                courseIds = courseIds,
            )
            map[instructor.id] = instructor
        }
        return map
    }

    private fun persist(map: Map<Int, Instructor>) {
        val array = JSONArray()
        for (instructor in map.values) {
            val o = JSONObject()
            o.put("id", instructor.id)
            o.put("lastName", instructor.lastName)
            o.put("firstMidName", instructor.firstMidName)
            o.put("hireDate", instructor.hireDate)
            if (instructor.officeLocation != null) {
                o.put("officeLocation", instructor.officeLocation)
            }
            val courseArray = JSONArray()
            for (cid in instructor.courseIds) {
                courseArray.put(cid)
            }
            o.put("courseIds", courseArray)
            array.put(o)
        }
        prefs.edit().putString("instructors", array.toString()).apply()
    }
}
