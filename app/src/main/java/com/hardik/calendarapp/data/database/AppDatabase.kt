package com.hardik.calendarapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.database.entity.Event
import java.util.Date

@Database(entities = [Event::class], version = 1, exportSchema = false)
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
                    "event_db")
                    //.addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Automatically drops and recreates the database
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the new columns as NOT NULL with default values
        database.execSQL("ALTER TABLE events ADD COLUMN year TEXT NOT NULL DEFAULT '2024'")
        database.execSQL("ALTER TABLE events ADD COLUMN month TEXT NOT NULL DEFAULT '0'")
        database.execSQL("ALTER TABLE events ADD COLUMN date TEXT NOT NULL DEFAULT '1'")
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