package com.hardik.calendarapp.di

import android.app.Application
import android.content.Context
import com.hardik.calendarapp.data.database.AppDatabase
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.repository.CalendarRepositoryImpl
import com.hardik.calendarapp.data.repository.EventRepositoryImpl
import com.hardik.calendarapp.domain.repository.CalendarRepository
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.DeleteCursorEventUseCase
import com.hardik.calendarapp.domain.use_case.SyncCursorEventsUseCase
import com.hardik.calendarapp.domain.use_case.UpdateCursorEventUseCase
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

//    @Provides
//    @Singleton
//    fun provideCursorEventRepository(@ApplicationContext context: Context): CalendarRepositoryImpl {
//        return CalendarRepositoryImpl(context)
//    }

    @Provides
    @Singleton
    fun provideCalendarRepository(context: Application, eventDao: EventDao, eventRepository: EventRepository): CalendarRepository {
        return CalendarRepositoryImpl(context, eventDao, eventRepository)
    }

    @Provides
    @Singleton
    fun provideSyncCalendarUseCase(repository: CalendarRepository): SyncCursorEventsUseCase {
        return SyncCursorEventsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteCalendarEventUseCase(repository: CalendarRepository): DeleteCursorEventUseCase {
        return DeleteCursorEventUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateCalendarEventUseCase(repository: CalendarRepository): UpdateCursorEventUseCase {
        return UpdateCursorEventUseCase(repository)
    }
}
