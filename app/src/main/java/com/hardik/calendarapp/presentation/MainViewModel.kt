package com.hardik.calendarapp.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Resource
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.domain.use_case.GetHolidayApiUseCase
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getHolidayApiUseCase: GetHolidayApiUseCase,
    private val eventRepository: EventRepository
) : ViewModel() {
    private val TAG = BASE_TAG + MainViewModel::class.java.simpleName

    private val _state = MutableStateFlow<DataState<HolidayApiDetail>>(DataState(isLoading = true))
    val state: StateFlow<DataState<HolidayApiDetail>> get() = _state

    init {
        getCalendarData()
    }

    private fun getCalendarData() {
        viewModelScope.launch {
            getHolidayApiUseCase.invoke().collect { result: Resource<HolidayApiDetail> ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = DataState(data = result.data); collectState()
                    }

                    is Resource.Error -> {
                        _state.value =
                            DataState(error = result.message ?: "An unexpected error occurred")
                    }

                    is Resource.Loading -> {
                        _state.value = DataState(isLoading = true)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun collectState() {
        viewModelScope.launch {
            state.collect { state ->
                when {
                    state.isLoading -> {
                        // Handle loading state (maybe trigger other UI-related actions or logging)
                        Log.d(TAG, "collectState: isLoading state: ${state.isLoading}")
                    }

                    state.error.isNotEmpty() -> {
                        // Handle error state (maybe trigger logging, analytics, etc.)
                        Log.d(TAG, "collectState: Error state: ${state.error}")
                    }

                    state.data != null -> {
                        // Handle success case (trigger actions like logging, analytics, etc.)
                        val calendarDetails = state.data
                        Log.d(TAG, "collectState: data available")

                        // Process events
                        val events: List<Event> = calendarDetails.items
                            .mapNotNull { item ->

                                    Event(
                                        id = DateUtil.stringToLong(item.start.date,DateUtil.DATE_FORMAT),
                                        title = item.summary,
                                        description = item.description,
                                        startDate = item.start.date,
                                        endDate = item.end.date,
                                        startTime = DateUtil.stringToLong(item.start.date, DateUtil.DATE_FORMAT),
                                        endTime = DateUtil.stringToLong(item.end.date, DateUtil.DATE_FORMAT),
                                        isHoliday = true
                                    )

                            }
                            .filterNotNull() // Filter out null values resulting from mapNotNull

                        // Insert events into your database or UI
                        insertEvents(events)
                    }
                }
            }
        }
    }


    fun insertEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.upsertEvent(event)
        }
    }

    private fun insertEvents(events: List<Event>) {
        viewModelScope.launch {
            eventRepository.upsertEvents(events)
        }
    }
}