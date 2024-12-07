package com.hardik.calendarapp.di

import com.hardik.calendarapp.data.repository.HolidayApiRepositoryImpl
import com.hardik.calendarapp.domain.repository.HolidayApiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: HolidayApiRepositoryImpl): HolidayApiRepository
}