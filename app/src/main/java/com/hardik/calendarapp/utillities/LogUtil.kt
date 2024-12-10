package com.hardik.calendarapp.utillities

import android.util.Log

object LogUtil {
    private const val LOG_CHUNK_SIZE = 4000 // Maximum size per log chunk

    /**
     * Logs a long message in chunks.
     * @param tag The log tag.
     * @param message The message to log.
     */
    fun logLongMessage(tag: String, message: String) {
        var start = 0
        val length = message.length
        while (start < length) {
            val end = (start + LOG_CHUNK_SIZE).coerceAtMost(length)
            Log.e(tag, message.substring(start, end))
            start = end
        }
    }
}
