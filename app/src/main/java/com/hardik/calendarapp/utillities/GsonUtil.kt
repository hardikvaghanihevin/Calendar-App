package com.hardik.calendarapp.utillities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GsonUtil {
    val gson: Gson = Gson()

    /**
     * Convert an object to a JSON string.
     */
    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    /**
     * Convert a JSON string to an object of the specified type.
     */
    inline fun <reified T> fromJson(json: String): T? {
        return if (json.isNullOrEmpty()) {
            null
        } else {
            gson.fromJson(json, T::class.java)
        }
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        return if (json.isNullOrEmpty()) {
            null
        } else {
            gson.fromJson(json, clazz)
        }
    }

    /**
     * Convert a JSON string to a list of objects of the specified type.
     */
    inline fun <reified T> fromJsonToList(json: String): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            gson.fromJson(json, type)
        }
    }
}