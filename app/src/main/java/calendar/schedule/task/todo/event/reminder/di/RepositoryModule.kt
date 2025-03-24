package calendar.schedule.task.todo.event.reminder.di

import calendar.schedule.task.todo.event.reminder.data.repository.HolidayApiRepositoryImpl
import calendar.schedule.task.todo.event.reminder.domain.repository.HolidayApiRepository
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