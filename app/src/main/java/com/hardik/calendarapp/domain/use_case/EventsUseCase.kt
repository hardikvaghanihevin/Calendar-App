package com.hardik.calendarapp.domain.use_case

import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


/**This method used getting month events. It's used in HomeViewModel*/
class GetMonthlyEventsUseCase @Inject constructor(private val repository: EventRepository) {
    operator fun invoke(startOfMonth: Long, endOfMonth: Long): Flow<List<Event>> = repository.getEventsForMonth(startOfMonth= startOfMonth, endOfMonth= endOfMonth)
}

class GetEventsByMonthOfYear @Inject constructor(private val repository: EventRepository){
    operator fun invoke(year: String, month: String): Flow<List<Event>> = repository.getEventsByYearAndMonth(year, month)
}

class GetAllEventsUseCase @Inject constructor(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<Event>> = repository.getAllEvents()
}

class GetEventByTitleAndType @Inject constructor(private val repository: EventRepository){
    operator fun invoke(title: String, type: EventType = EventType.PERSONAL): Flow<Event?> = repository.getEventByTitleAndType(title, type)
}