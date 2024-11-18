package com.hardik.calendarapp.di

import android.content.Context
import com.hardik.calendarapp.data.database.AppDatabase
import com.hardik.calendarapp.data.database.dao.EventDao
import com.hardik.calendarapp.data.repository.EventRepositoryImpl
import com.hardik.calendarapp.domain.repository.EventRepository
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
    fun provideEventRepository(eventDao: EventDao): EventRepository {
        return EventRepositoryImpl(eventDao)
    }
}
