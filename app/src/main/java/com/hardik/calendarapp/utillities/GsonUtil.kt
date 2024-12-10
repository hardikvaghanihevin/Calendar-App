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
    inline fun <reified T> fromJson(json: String): T {
        return gson.fromJson(json, T::class.java)
    }

    /**
     * Convert a JSON string to a list of objects of the specified type.
     */
    inline fun <reified T> fromJsonToList(json: String): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, type)
    }
}
/**
 * example:
 ` val json = GsonUtil.toJson(calendarEvent)
 ` println("JSON: $json")
 `
 ` val jsonList = GsonUtil.toJson(eventsList)
 ` println("JSON List: $jsonList")
 */