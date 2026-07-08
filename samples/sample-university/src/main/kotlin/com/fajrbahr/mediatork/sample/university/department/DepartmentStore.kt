package com.fajrbahr.mediatork.sample.university.department

import android.content.SharedPreferences
import com.fajrbahr.mediatork.sample.university.department.model.Department
import org.json.JSONArray
import org.json.JSONObject

class DepartmentStore(private val prefs: SharedPreferences) {

    fun nextId(): Int {
        val next = prefs.getInt("department_id_seq", 0) + 1
        prefs.edit().putInt("department_id_seq", next).apply()
        return next
    }

    fun save(department: Department) {
        val all = loadAll().toMutableMap()
        all[department.id] = department
        persist(all.values)
    }

    fun findById(id: Int): Department? = loadAll()[id]

    fun findAll(): List<Department> = loadAll().values.sortedBy { it.id }

    fun delete(id: Int) {
        val all = loadAll().toMutableMap()
        all.remove(id)
        persist(all.values)
    }

    private fun loadAll(): Map<Int, Department> {
        val json = prefs.getString("departments", null) ?: return emptyMap()
        val array = JSONArray(json)
        val map = mutableMapOf<Int, Department>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val dept = Department(
                id = o.getInt("id"),
                name = o.getString("name"),
                budget = o.getDouble("budget"),
                startDate = o.getString("startDate"),
                administratorId = if (o.has("administratorId")) o.getInt("administratorId") else null,
            )
            map[dept.id] = dept
        }
        return map
    }

    private fun persist(departments: Collection<Department>) {
        val array = JSONArray()
        for (dept in departments) {
            val o = JSONObject()
            o.put("id", dept.id)
            o.put("name", dept.name)
            o.put("budget", dept.budget)
            o.put("startDate", dept.startDate)
            if (dept.administratorId != null) {
                o.put("administratorId", dept.administratorId)
            }
            array.put(o)
        }
        prefs.edit().putString("departments", array.toString()).apply()
    }
}
