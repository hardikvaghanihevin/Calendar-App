package com.hardik.calendarapp.domain.use_case

import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


/**This method used getting month events. It's used in HomeViewModel*/
class GetMonthlyEventsUseCase @Inject constructor(
    private val repository: EventRepository
) {
    operator fun invoke(startOfMonth: Long, endOfMonth: Long): Flow<List<Event>> = repository.getEventsForMonth(startOfMonth= startOfMonth, endOfMonth= endOfMonth)
}

class ObserveAllEventsUseCase @Inject constructor(
    private val repository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> = repository.getAllEvents()
}

class GetEventIndicatorMapUseCase @Inject constructor(
    private val repository: EventRepository
){
    operator fun invoke(year: String, month: String): Flow<MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>> {
        return repository.getEventsByYearAndMonth(year, month).map { events ->
            val startTime = System.nanoTime()

            // Create a map to store events in a nested structure: year -> month -> day -> event
            val mapOfEvents = mutableMapOf<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>()

            events.forEach { event ->
                val yearKey = event.year
                val monthKey = event.month
                val dayKey = event.date

                // Build the nested map structure, getOrPut ensures the maps exist
                mapOfEvents
                    .getOrPut(yearKey) { mutableMapOf() }
                    .getOrPut(monthKey) { mutableMapOf() }
                    .put(dayKey, "$year-$monthKey-$dayKey") // replace 'event' with "$year-$monthKey-$dayKey" -> '2024-0-1
            }

            val endTime = System.nanoTime()
            //Log.d(TAG, "repository.getEventsByYearAndMonth(year, month): execution time: ${(endTime - startTime)} ns, ${(endTime - startTime) / 1_000} µs, ${(endTime - startTime) / 1_000_000} ms")

            mapOfEvents
        }
    }
}

//class FetchHolidayEventsUseCase @Inject constructor(
//    private val repository: EventRepository
//) {
//    operator fun invoke(): Flow<List<Event>> = repository.getHolidayEvents()
//}
