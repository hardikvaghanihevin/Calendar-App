package com.hardik.calendarapp.domain.use_case

import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


/**This method used getting month events. It's used in HomeViewModel*/
class FetchEventsForMonthUseCase @Inject constructor(
    private val repository: EventRepository
) {
    operator fun invoke(startOfMonth: Long, endOfMonth: Long): Flow<List<Event>> = repository.getEventsForMonth(startOfMonth= startOfMonth, endOfMonth= endOfMonth)
}

class ObserveAllEventsUseCase @Inject constructor(
    private val repository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> = repository.getAllEvents()
}

//class FetchHolidayEventsUseCase @Inject constructor(
//    private val repository: EventRepository
//) {
//    operator fun invoke(): Flow<List<Event>> = repository.getHolidayEvents()
//}