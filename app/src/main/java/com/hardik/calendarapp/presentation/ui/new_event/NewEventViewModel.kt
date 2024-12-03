package com.hardik.calendarapp.presentation.ui.new_event

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.GetEventsByMonthOfYear
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NewEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,// For Database compatibility
    private val getEventsByMonthOfYear: GetEventsByMonthOfYear,
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

    //Todo: Event start date
    private val _startDate= MutableStateFlow<Long>(Calendar.getInstance().timeInMillis)
    val startDate: StateFlow<Long> = _startDate
    fun updateStartDate(startDate: Long) {
        Log.i(TAG, "updateStartDate: $startDate")
        viewModelScope.launch {
            _startDate.value = startDate
        }
    }

    //todo: Event end date
    private val _endDate= MutableStateFlow<Long>(Calendar.getInstance().timeInMillis)
    val endDate: StateFlow<Long> = _endDate
    fun updateEndDate(endDate: Long) {
        Log.i(TAG, "updateEndDate: $endDate")
        viewModelScope.launch {
            _endDate.value = endDate
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

    // Set default start time to current time (hour and minute)
    private val defaultStartTime = calendar.apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Set default end time to one hour ahead
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

    fun insertCustomEvent(){
        val currentEpochTime = System.currentTimeMillis()

        val date: Triple<String, String, String> = DateUtil.epochToDateTriple(
            startDate.value
        )

        val event = Event(
            id = "$currentEpochTime | ${title.value}",
            title = title.value,
            description = description.value,
            startDate = DateUtil.longToString(startDate.value, DateUtil.DATE_FORMAT),
            endDate = DateUtil.longToString(endDate.value, DateUtil.DATE_FORMAT),
            startTime = startTime.value,
            endTime = endTime.value,
            year = date.first,
            month = date.second,
            date = date.third,
            eventType = EventType.PERSONAL,
            isHoliday = false
        )

        insertEvent(event)
    }

    //----------------------------------------------------------------//

    fun insertEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.upsertEvent(event)
        }
    }
}