package com.hardik.calendarapp.presentation.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardik.calendarapp.common.Constants.BASE_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(): ViewModel() {
    private val TAG = BASE_TAG + CalendarViewModel::class.java.simpleName

    private val _yearState = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.YEAR))
    private val _monthState = MutableStateFlow<Int>(Calendar.getInstance().get(Calendar.MONTH))

    val yearState = _yearState
    val monthState = _monthState
    fun updateYear(year: Int) {
        Log.i(TAG, "updateYear: $year")
        viewModelScope.launch {
            _yearState.emit(year)
        }
    }

    fun updateMonth(month: Int) {
        Log.i(TAG, "updateMonth: $month")
        viewModelScope.launch {
            _monthState.emit(month)
        }
    }
}