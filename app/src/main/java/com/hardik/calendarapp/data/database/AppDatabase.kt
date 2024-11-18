package com.hardik.calendarapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.database.entity.Event
import java.util.Date

@Database(entities = [Event::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao() : EventDao

    companion object {
        @Volatile
        private var INSTANCE : AppDatabase? = null

        fun getDatabase(context : Context) : AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "event_db").build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DateConverter {

    // Convert Long (timestamp) to Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    // Convert Date to Long (timestamp)
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}