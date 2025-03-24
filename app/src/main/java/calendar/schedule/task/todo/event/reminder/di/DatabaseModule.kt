package calendar.schedule.task.todo.event.reminder.di

import android.content.Context
import calendar.schedule.task.todo.event.reminder.data.database.AppDatabase
import calendar.schedule.task.todo.event.reminder.data.database.dao.EventDao
import calendar.schedule.task.todo.event.reminder.data.repository.EventRepositoryImpl
import calendar.schedule.task.todo.event.reminder.domain.repository.EventRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideEventDao(appDatabase: AppDatabase): EventDao {
        return appDatabase.eventDao()
    }

    @Provides
    @Singleton
    fun provideEventRepository(eventDao: EventDao, @ApplicationContext context: Context): EventRepository {
        return EventRepositoryImpl(eventDao, context)
    }
}
