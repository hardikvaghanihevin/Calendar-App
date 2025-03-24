package calendar.schedule.task.todo.event.reminder.domain.repository

import calendar.schedule.task.todo.event.reminder.data.remote.dto.HolidayApiDto

interface HolidayApiRepository {
    suspend fun getHolidayEvents(countryCode: String, languageCode: String ): HolidayApiDto?
}