package com.hardik.calendarapp.presentation.ui.new_event

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.EVENT_INSERT_SUCCESSFULLY
import com.hardik.calendarapp.common.Constants.EVENT_UPDATE_SUCCESSFULLY
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.GetEventByTitleAndType
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NewEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,// For Database compatibility
    private val getEventByTitleAndType: GetEventByTitleAndType,
): ViewModel() {
    private val TAG = BASE_TAG + NewEventViewModel::class.java.simpleName


    private val _yearState = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.YEAR))
    val yearState: StateFlow<Int> = _yearState

    fun updateYear(year: Int) {
        Log.i(TAG, "updateYear: $year")
        viewModelScope.launch {
            _yearState.value = year
        }
    }

    //----------------------------------------------------------------//
    val date: Pair<Long, Long> = DateUtil.getStartAndEndOfDay(Calendar.getInstance().timeInMillis)
    //Todo: Event start date
    private val _startDate= MutableStateFlow<Long>(date.first)//date.first is the statDate
    val startDate: StateFlow<Long> = _startDate
    fun updateStartDate(startDate: Long) {
        val date = DateUtil.getStartAndEndOfDay(startDate)////date.first is the statDate
        Log.i(TAG, "updateStartDate: ${date.first}")
        viewModelScope.launch {
            _startDate.value = date.first
        }
    }

    //todo: Event end date
    private val _endDate= MutableStateFlow<Long>(date.second)//date.second is the endDate
    val endDate: StateFlow<Long> = _endDate
    fun updateEndDate(endDate: Long) {
        val date = DateUtil.getStartAndEndOfDay(endDate)//date.second is the endDate
        Log.i(TAG, "updateEndDate: ${date.second}")
        viewModelScope.launch {
            _endDate.value = date.second
        }
    }

    //----------------------------------------------------------------//

    //todo: Event All-Day
    private val _isAllDay = MutableStateFlow(false) // Default to false (not all day)
    val isAllDay: StateFlow<Boolean> = _isAllDay

    fun updateAllDayStatus(isAllDay: Boolean) {
        Log.i(TAG, "updateAllDayStatus: $isAllDay")
        viewModelScope.launch {
            _isAllDay.value = isAllDay
        }
        val mergeDataTime = DateUtil.mergeDateAndTime(startDate.value, startTime.value)
        Log.e(TAG, "updateAllDayStatus: $mergeDataTime", )
        Log.e(TAG, "updateAllDayStatus: ${DateUtil.separateDateTime(mergeDataTime)}")
    }

    //----------------------------------------------------------------//

    private val calendar = Calendar.getInstance()

    // Set default start time to current time (hour and minute) hh:mm a
    private val defaultStartTime = calendar.apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Set default end time to one hour ahead (hour and minute) hh:mm a
    private val defaultEndTime = calendar.apply {
        add(Calendar.HOUR_OF_DAY, 1)
    }.timeInMillis

    //todo: Event start time
    private val _startTime= MutableStateFlow<Long>(defaultStartTime)
    val startTime: StateFlow<Long> = _startTime

    //todo: Event end time
    private val _endTime= MutableStateFlow<Long>(defaultEndTime)
    val endTime: StateFlow<Long> = _endTime

    fun updateStartTime(startTime: Long) {
        Log.i(TAG, "updateStartTime: $startTime")
        viewModelScope.launch {
            _startTime.value = startTime
        }
    }

    fun updateEndTime(endTime: Long) {
        Log.i(TAG, "updateEndTime: $endTime")
        viewModelScope.launch {
            _endTime.value = endTime
        }
    }

    //todo: Event title
    private val _title= MutableStateFlow("")
    val title: StateFlow<String> = _title

    fun updateTitle(title: String) {
        Log.i(TAG, "updateTitle: $title")
        viewModelScope.launch {
            _title.value = title
        }
    }

    //todo: Event description
    private val _description= MutableStateFlow("")
    val description: StateFlow<String> = _description

    fun updateDescription(description: String) {
        Log.i(TAG, "updateDescription: $description")
        viewModelScope.launch {
            _description.value = description
        }
    }

    private suspend fun validateEvent(eventId: String? = null): String? {
        // Validate event title
        if (title.value.isBlank()) {
            return "Event title cannot be empty."
        }

        // Validate start and end dates
        if (startDate.value > endDate.value) {
            Log.e(TAG, "validateEvent: (startData > endDate) : ${startDate.value} ~ ${endDate.value}")
            return "Start date cannot be after end date."
        }

        // Validate start and end times (if not an all-day event)
        if (!isAllDay.value && startTime.value >= endTime.value) {
            Log.e(TAG, "validateEvent: (startTime > endTime) : ${startTime.value} ~ ${endTime.value}")
            return "Start time cannot be after end time."
        }

        // Check if the event is new or being updated
        val existingEvent = if (eventId == null) {
            // Creating a new event, check if a similar event exists
            getEventByTitleAndType.invoke(title = title.value).firstOrNull()
        } else {
            // Updating an existing event, allow duplicates only for the current event ID
            val result = getEventByTitleAndType.invoke(title = title.value).firstOrNull()
            if (result?.id == eventId) {
                // If the event's ID matches the current event ID, return null (no conflict)
                null
            } else {
                // Otherwise, return the conflicting event (indicating a validation error)
                result
            }
        }
        //Log.d(TAG, "validateEvent: $existingEvent")

        if (existingEvent != null) {
            return "An event with this title and type already exists."
        }

        return null // No validation errors
    }


    suspend fun insertCustomEvent(id: String?): String{
        Log.d(TAG, "insertCustomEvent: ")
        val errorMessage = validateEvent(eventId = id)
        Log.e(TAG, "insertCustomEvent: $errorMessage", )
        if (errorMessage != null) {
            return errorMessage
        }

        val currentEpochTime = System.currentTimeMillis()

        val date: Triple<String, String, String> = DateUtil.epochToDateTriple(
            startDate.value
        )

        val event = Event(
            id = id.takeIf { id != null }?: "$currentEpochTime | ${title.value}",
            title = title.value,
            description = description.value,
            startDate = DateUtil.longToString(startDate.value, DateUtil.DATE_FORMAT_yyyy_MM_dd),
            endDate = DateUtil.longToString(endDate.value, DateUtil.DATE_FORMAT_yyyy_MM_dd),
            startTime = startDate.value.takeIf { isAllDay.value } ?: startTime.value,//hh:mm a
            endTime = endDate.value.takeIf { isAllDay.value } ?: endTime.value,//hh:mm a
            year = date.first,
            month = date.second,
            date = date.third,
            eventType = EventType.PERSONAL,
            isHoliday = false
        )

        insertEvent(event)
        return EVENT_INSERT_SUCCESSFULLY.takeIf { id == null } ?: EVENT_UPDATE_SUCCESSFULLY// Event inserted/update successfully
    }

    fun resetEventState() {
        val date = DateUtil.getStartAndEndOfDay(Calendar.getInstance().timeInMillis)

        viewModelScope.launch {
            _startDate.value = date.first
            _endDate.value = date.second

            val calendar = Calendar.getInstance()
            _startTime.value = calendar.apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            _endTime.value = calendar.apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }.timeInMillis

            _title.value = ""
            _description.value = ""
            _isAllDay.value = false
        }
    }

    //----------------------------------------------------------------//

    private fun insertEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.upsertEvent(event)
        }
    }
}