package calendar.schedule.task.todo.event.reminder.domain.use_case

import calendar.schedule.task.todo.event.reminder.data.database.entity.Event
import calendar.schedule.task.todo.event.reminder.domain.repository.EventRepository
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
    operator fun invoke(eventsQuery: String?): Flow<List<Event>> = repository.getAllEvents(eventsQuery = eventsQuery)
}
