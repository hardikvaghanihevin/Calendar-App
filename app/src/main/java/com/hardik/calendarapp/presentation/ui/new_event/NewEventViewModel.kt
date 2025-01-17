package com.hardik.calendarapp.presentation.ui.new_event

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.EVENT_INSERT_SUCCESSFULLY
import com.hardik.calendarapp.common.Constants.EVENT_UPDATE_SUCCESSFULLY
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.data.database.entity.SourceType
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.GetEventByTitleAndType
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

            //This is for current start time set when date change
            updateStartTime( DateUtil.mergeDateAndTime(dateEpoch =  date.first, timeEpoch = _startTime.value) )
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

            //This is for current start time set when date change
            updateEndTime( DateUtil.mergeDateAndTime(dateEpoch =  date.second, timeEpoch = _endTime.value) )

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

    //todo: Event Repeat (None, Once, Daly, Weekly, Monthly, Yearly)
    private val _repeatOption = MutableStateFlow(RepeatOption.NEVER)//ONCE
    val repeatOption:StateFlow<RepeatOption> = _repeatOption

    fun updateRepeatOption(repeatOption: RepeatOption){
        Log.d(TAG, "updateRepeatOption: $repeatOption")
        viewModelScope.launch {
            _repeatOption.value = repeatOption
        }
    }

    //todo: Event Alert (Before 5 min,10 min, 15 min, 1 hour, 1 day...)
    private val _alertOffset = MutableStateFlow(AlertOffset.AT_TIME)
    val alertOffset: StateFlow<AlertOffset> = _alertOffset

    fun updateAlertOffset(alertOffset: AlertOffset){
        Log.d(TAG, "updateAlertOffset: $alertOffset")
        viewModelScope.launch {
            if(alertOffset != AlertOffset.BEFORE_CUSTOM_TIME) updateCustomAlertOffset()
            _alertOffset.value = alertOffset
        }
    }

    private val _customAlertOffset = MutableStateFlow<Long?>(null)
    val customAlertOffset: StateFlow<Long?> = _customAlertOffset

    fun updateCustomAlertOffset(customAlertOffset: Long? = null) {
        Log.d(TAG, "updateCustomAlertOffset: $customAlertOffset")
        viewModelScope.launch {
            if (customAlertOffset != null) {
                updateAlertOffset(AlertOffset.BEFORE_CUSTOM_TIME)
            }
            _customAlertOffset.value = customAlertOffset
        }
    }

    private val triggerTime: StateFlow<Long?> = combine(_alertOffset, _startTime) { alertOffset, startTime ->
        Log.e(TAG, "trigger: alert:$alertOffset | startTime:$startTime", )

        val alertOffsetValue = if (alertOffset == AlertOffset.BEFORE_CUSTOM_TIME) { _customAlertOffset.value }
        else { AlertOffsetConverter.toMilliseconds(alertOffset) }

        Log.i(TAG, "trigger Time alertOffsetValue: $alertOffsetValue")
        if (alertOffsetValue != null) {
            startTime - alertOffsetValue
        } else {
            null
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        AlertOffsetConverter.toMilliseconds(alertOffset.value)//null // Default value, if no trigger time has been set
    )

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


    suspend fun insertCustomEvent(context: Context, id: String?): String{
        Log.d(TAG, "insertCustomEvent: ")
        val errorMessage = validateEvent(eventId = id)
        Log.e(TAG, "insertCustomEvent(): validateEvent message:- $errorMessage", )
        if (errorMessage != null) {
            return errorMessage
        }

        val currentEpochTime = System.currentTimeMillis()

        val date: Triple<String, String, String> = DateUtil.epochToDateTriple(
            startDate.value
        )

        // Get the latest trigger time value
        val latestTriggerTime = triggerTime.firstOrNull() // Use `firstOrNull` to get the latest value synchronously

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
            isHoliday = false,
            sourceType = SourceType.LOCAL,
            repeatOption = repeatOption.value,
            alertOffset = alertOffset.value,
            customAlertOffset = customAlertOffset.value,
            eventId = currentEpochTime,
            triggerTime = latestTriggerTime ?: 0L, // todo: set triggerTime as start time
        )

        insertEvent(event)
        return EVENT_INSERT_SUCCESSFULLY.takeIf { id == null } ?: EVENT_UPDATE_SUCCESSFULLY// Event inserted/update successfully
    }

    fun resetEventState() {
        Log.d(TAG, "resetEventState: ")
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
            _repeatOption.value = RepeatOption.NEVER//ONCE
            _alertOffset.value = AlertOffset.AT_TIME
            _customAlertOffset.value = null
        }
    }

    //----------------------------------------------------------------//

    fun cancelAlarm(id: String){
        viewModelScope.launch {
            eventRepository.cancelAlarm(id = id)// cancel when update single event from newEventFrag
        }
    }
    private val insertEventsMutex = Mutex()
    private fun insertEvent(event: Event) {
        viewModelScope.launch {
            insertEventsMutex.withLock {
                try {
                    val updatedEvent = coroutineScope {
                            async(Dispatchers.Default) {
                                var nextTriggerTime = event.triggerTime

                                // Calculate nextTriggerTime if needed
                                if (nextTriggerTime <= System.currentTimeMillis() && event.repeatOption != RepeatOption.NEVER) {
                                    val calculatedTriggerTime = DateUtil.calculateNextOccurrence(nextTriggerTime, event.repeatOption)
                                    if (calculatedTriggerTime != null) {
                                        nextTriggerTime = calculatedTriggerTime
                                    }
                                }

                                // Return the updated event
                                event.copy(triggerTime = nextTriggerTime)
                            }.await() // Collect all updated events
                    }

                    withContext(Dispatchers.IO) { eventRepository.upsertEvent(updatedEvent) }

                } catch (e: Exception) {
                    // Handle any errors
                    Log.e(TAG,"InsertEvents - Error inserting events", e)
                }
            }
        }
    }

    fun deleteEvent(argEvent: Event) {
        viewModelScope.launch {
            eventRepository.deleteEvent(argEvent)
        }

    }
}