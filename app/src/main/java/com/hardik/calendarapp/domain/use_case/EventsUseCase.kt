package com.hardik.calendarapp.domain.use_case

import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.domain.repository.CalendarRepository
import com.hardik.calendarapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


/**This method used getting month events. It's used in HomeViewModel*/

class GetEventsByMonthOfTheYear @Inject constructor(private val repository: EventRepository){//todo: use in CalendarMonth1Fragment for onMonthSwipe or onMonthClick
    operator fun invoke(year: String, month: String): Flow<List<Event>> = repository.getEventsByMonthOfTheYear(year = year, month = month)
}

class GetEventsByDateOfMonthOfTheYear @Inject constructor(private val repository: EventRepository){//todo: use in CalendarMonth1Fragment for onDateClick
    operator fun invoke(year: String, month: String, date: String): Flow<List<Event>> = repository.getEventsByDateOfMonthOfTheYear(year = year, month = month, date = date)
}

class GetAllEventsUseCase @Inject constructor(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<Event>> = repository.getAllEvents()
}

class SyncCursorEventsUseCase @Inject constructor(private val calendarRepository: CalendarRepository) {
    suspend operator fun invoke() { calendarRepository.syncCursorEvents() }
}
class UpdateCursorEventUseCase @Inject constructor(private val calendarRepository: CalendarRepository) {
    suspend operator fun invoke(event: Event): Boolean { return calendarRepository.updateCursorEvent(event) }
}

class DeleteCursorEventUseCase @Inject constructor(private val calendarRepository: CalendarRepository) {
    suspend operator fun invoke(eventId: Long): Boolean { return calendarRepository.deleteCursorEvent(eventId) }
}

