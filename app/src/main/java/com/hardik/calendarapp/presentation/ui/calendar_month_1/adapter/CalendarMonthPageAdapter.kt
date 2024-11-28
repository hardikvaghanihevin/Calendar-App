package com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter

import android.annotation.SuppressLint
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

//    private var eventsOfDate: List<String> = emptyList()
//    @SuppressLint("NotifyDataSetChanged")
//    fun updateEventsOfDate(dates: List<String>) {
//        this.eventsOfDate = dates
//        notifyDataSetChanged()
//    }
    private var eventsOfDateMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()
    @SuppressLint("NotifyDataSetChanged")
    fun updateEventsOfDate(dates: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()) {
        this.eventsOfDateMap = dates
        notifyDataSetChanged()
    }

    companion object {
        const val FAKE_TOTAL_COUNT = Int.MAX_VALUE // Simulate infinite pages
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding =
            ItemMonthPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        //val monthIndex = position % 12
        holder.bind(eventsOfDateMap)
    }

    override fun getItemCount(): Int = FAKE_TOTAL_COUNT

    inner class MonthViewHolder(val binding: ItemMonthPage1Binding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(eventMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()) {
            binding.apply {
                customView.apply {
                    this.currentYear = currentYear
                    this.currentMonth = currentMonth
                    enableTouchEventHandling(enable = true)
                    this.eventDateList = this@CalendarMonthPageAdapter.eventsOfDateMap
                    postInvalidate() // Redraw the custom view if needed
                    configureCustomViewCallback?.invoke(this) // Optional callback for further customization
                }
            }
        }
    }

    private var configureCustomViewCallback: ((CustomViewMonth) -> Unit)? = null
    fun configureCustomView(callback: (CustomViewMonth) -> Unit) {
        this.configureCustomViewCallback = callback
    }
}



