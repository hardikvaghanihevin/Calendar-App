package calendar.schedule.task.todo.event.reminder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import calendar.schedule.task.todo.event.reminder.data.database.dao.EventDao
import calendar.schedule.task.todo.event.reminder.data.database.entity.Event

@Database(entities = [Event::class], version = 1, exportSchema = false)
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
                    .fallbackToDestructiveMigration() // Automatically drops and recreates the database
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}