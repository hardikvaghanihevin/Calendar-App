package com.hardik.calendarapp.presentation.ui.new_event

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.R
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
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.mergeDateAndTime
import com.hardik.calendarapp.utillities.DateUtil.separateDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NewEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,// For Database compatibility
): ViewModel() {
    private val TAG = BASE_TAG + NewEventViewModel::class.java.simpleName


    private val _yearState = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.YEAR))
    val yearState: StateFlow<Int> = _yearState

    fun updateYear(year: Int) {
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
        viewModelScope.launch {
            _isAllDay.value = isAllDay
        }
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
        viewModelScope.launch {
            _startTime.value = startTime
        }
    }

    fun updateEndTime(endTime: Long) {
        viewModelScope.launch {
            _endTime.value = endTime
        }
    }

    //todo: Event title
    private val _title= MutableStateFlow("")
    val title: StateFlow<String> = _title

    fun updateTitle(title: String) {
        viewModelScope.launch {
            _title.value = title
        }
    }

    //todo: Event description
    private val _description= MutableStateFlow("")
    val description: StateFlow<String> = _description

    fun updateDescription(description: String) {
        viewModelScope.launch {
            _description.value = description
        }
    }

    //todo: Event Repeat (None, Once, Daly, Weekly, Monthly, Yearly)
    private val _repeatOption = MutableStateFlow(RepeatOption.NEVER)//ONCE
    val repeatOption:StateFlow<RepeatOption> = _repeatOption

    fun updateRepeatOption(repeatOption: RepeatOption){
        viewModelScope.launch {
            _repeatOption.value = repeatOption
        }
    }

    //todo: Event Alert (Before 5 min,10 min, 15 min, 1 hour, 1 day...)
    private val _alertOffset = MutableStateFlow(AlertOffset.AT_TIME_OF_EVENT)
    val alertOffset: StateFlow<AlertOffset> = _alertOffset

    fun updateAlertOffset(alertOffset: AlertOffset){
        viewModelScope.launch {
            if(alertOffset != AlertOffset.BEFORE_CUSTOM_TIME) updateCustomAlertOffset()
            _alertOffset.value = alertOffset
        }
    }

    private val _customAlertOffset = MutableStateFlow<Long?>(null)
    val customAlertOffset: StateFlow<Long?> = _customAlertOffset

    fun updateCustomAlertOffset(customAlertOffset: Long? = null) {
        viewModelScope.launch {
            if (customAlertOffset != null) {
                updateAlertOffset(AlertOffset.BEFORE_CUSTOM_TIME)
            }
            _customAlertOffset.value = customAlertOffset
        }
    }


    private suspend fun validateEvent(context: Context, eventId: String? = null): String? {
        // Validate event title
        if (title.value.isBlank()) {
            return context.resources.getString(R.string.event_title_cannot_empty)
        }

        // Validate start and end dates
        if (startDate.value > endDate.value) {
            return context.resources.getString(R.string.start_date_cannot_be_after_end_date)
        }

        // Validate start and end times (if not an all-day event)
        if (!isAllDay.value && startTime.value >= endTime.value) {
            return context.resources.getString(R.string.start_time_cannot_be_after_end_time)
        }

        return null // No validation errors
    }


    suspend fun insertCustomEvent(context: Context, id: String?): String{
        val errorMessage = validateEvent(context = context, eventId = id)
        if (errorMessage != null) {
            return errorMessage
        }

        val currentEpochTime = System.currentTimeMillis()

        val date: Triple<String, String, String> = DateUtil.epochToDateTriple(
            startDate.value
        )

        // Get the latest trigger time value
        val getTriggerTime = getTriggerTime(alertOffset.value, startTime.value, startDate.value, isAllDay.value)
        
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
            triggerTime = getTriggerTime, // todo: set triggerTime as start time
        )

        insertEvent(event)
        return EVENT_INSERT_SUCCESSFULLY.takeIf { id == null } ?: EVENT_UPDATE_SUCCESSFULLY// Event inserted/update successfully
    }

    private fun getTriggerTime(alert: AlertOffset, startTime: Long, startDate: Long, isAllDay: Boolean): Long {

        val timeStamp = startDate.takeIf { isAllDay } ?: mergeDateAndTime(startDate, separateDateTime(startTime).second)
        val triggerTime = timeStamp - (AlertOffsetConverter.toMilliseconds(alert) ?: 0L)
        return triggerTime
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
            _repeatOption.value = RepeatOption.NEVER
            _alertOffset.value = AlertOffset.AT_TIME_OF_EVENT
            _customAlertOffset.value = null
        }
    }

    //----------------------------------------------------------------//

    fun cancelAlarm(event: Event){
        viewModelScope.launch {
            eventRepository.cancelAlarm(event = event)// cancel when update single event from newEventFrag
        }
    }

    private val insertEventsMutex = Mutex()
    private fun insertEvent(event: Event) {
        viewModelScope.launch {
            insertEventsMutex.withLock {
                try {
                    val updatedEvent = coroutineScope {
                            async(Dispatchers.Default) {
                                val nextTriggerTime: Long

                                val calculatedTriggerTime = DateUtil.calculateNextOccurrence(event.triggerTime, event.repeatOption)

                                nextTriggerTime = calculatedTriggerTime ?: event.triggerTime

                                event.copy(triggerTime = nextTriggerTime)
                            }.await() // Collect all updated events
                    }

                    withContext(Dispatchers.IO) { eventRepository.upsertEvent(updatedEvent) }

                } catch (e: Exception) {
                    // InsertEvents - Error inserting events
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