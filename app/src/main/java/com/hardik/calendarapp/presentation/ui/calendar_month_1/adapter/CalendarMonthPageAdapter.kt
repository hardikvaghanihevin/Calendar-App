package com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.databinding.ItemMonthPage1Binding
import com.hardik.calendarapp.presentation.ui.custom_view.CustomViewMonth

class CalendarMonthPageAdapter() :
    RecyclerView.Adapter<CalendarMonthPageAdapter.MonthViewHolder>() {
    private val TAG = BASE_TAG + CalendarMonthPageAdapter::class.java.simpleName

    private var yearMonthPairList : List<Pair<Int, Int>> = emptyList()
    private var eventsOfDateMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()

    private var selectedDate: String? = null
    // Method to update yearList and refresh the RecyclerView
    // Update year and month list
    @SuppressLint("NotifyDataSetChanged")
    fun updateYearMonthPairList(newYearMonthPairList: List<Pair<Int, Int>>) {
        yearMonthPairList = newYearMonthPairList
        notifyDataSetChanged()
    }

    // Update event date map
    @SuppressLint("NotifyDataSetChanged")
    fun updateEventsOfDate(dates: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>) {
        eventsOfDateMap = dates
        notifyDataSetChanged()
    }

    // Set selected date
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedDate(yyyy_mm_dd: String?) {
        Log.e(TAG, "setSelectedDate: $yyyy_mm_dd")
        if (selectedDate != yyyy_mm_dd) {
            selectedDate = yyyy_mm_dd
            notifyDataSetChanged()
        }
    }

    private var firstDayOfTheWeek = "Sunday"
    @SuppressLint("NotifyDataSetChanged")
    fun updateFirstDayOfTheWeek(firstDay: String) {
        firstDayOfTheWeek = firstDay
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        //val monthIndex = position % 12
        val (year, month) = yearMonthPairList[position]

        val weekStart = when(firstDayOfTheWeek){
            "Sunday" -> CustomViewMonth.WeekStart.SUNDAY
            "Monday" -> CustomViewMonth.WeekStart.MONDAY
            else -> { CustomViewMonth.WeekStart.SATURDAY }
        }
        holder.binding.also {

            val cvm: CustomViewMonth = it.customView.apply {
                this.currentYear = year
                this.currentMonth = month
                weekStart(weekStart)
                this.monthNameWithYear = true
                this.selectedDate = this@CalendarMonthPageAdapter.selectedDate//"2024-11-25"
                enableTouchEventHandling(enable = true)
                this.eventDateList = eventsOfDateMap
                postInvalidate() // Redraw the custom view if needed

                selectedDate
            }
            configureCustomViewCallback?.invoke(cvm) // Optional callback for further customization
        }
        //holder.bind(eventsOfDateMap)
    }

    override fun getItemCount(): Int = yearMonthPairList.size

    inner class MonthViewHolder(val binding: ItemMonthPage1Binding) : RecyclerView.ViewHolder(binding.root)

    private var configureCustomViewCallback: ((CustomViewMonth) -> Unit)? = null
    fun configureCustomView(callback: (CustomViewMonth) -> Unit) {
        this.configureCustomViewCallback = callback
    }
}

private var dateSelectedCallback: ((String?) -> Unit)? = null
fun setDateSelectedCallback(callback: (String?) -> Unit) {
    dateSelectedCallback = callback
}

// Call this method when a date is selected
fun notifyDateSelected(date: String?) {
    dateSelectedCallback?.invoke(date)
}



