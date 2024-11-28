package com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ItemMonthPage1Binding
import com.hardik.calendarapp.presentation.ui.custom_view.CustomViewMonth

class CalendarMonthPageAdapter() :
    RecyclerView.Adapter<CalendarMonthPageAdapter.MonthViewHolder>() {
    private val TAG = BASE_TAG + CalendarMonthPageAdapter::class.java.simpleName

    var eventsOfDateMap: List<Map<String, String>> = listOf(emptyMap())
    private var eventsOfDate: List<String> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateEventsOfDateMap(dates: List<Map<String, String>>) {
        this.eventsOfDateMap = dates
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateEventsOfDate(dates: List<String>) {
        this.eventsOfDate = dates
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
        holder.bind(eventsOfDate)
    }

    override fun getItemCount(): Int = FAKE_TOTAL_COUNT

    inner class MonthViewHolder(val binding: ItemMonthPage1Binding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(events: List<String>) {
            binding.apply {
                customView.apply {
                    enableTouchEventHandling(enable = true)
                    //eventDateListMap = eventsOfDateMap
                    updateEventsOfDate(events)
                    this.eventDateList = this@CalendarMonthPageAdapter.eventsOfDate
                    postInvalidate() // Redraw the custom view if needed
                    configureCustomViewCallback?.invoke(this) // Optional callback for further customization
                }
                Log.e(TAG, "bind: $eventsOfDate size- ${eventsOfDate.size}")
            }
        }

        // Bind the current year, month, and events for the month
        fun bind(year: Int, month: Int, events: List<String>) {
            binding.customView.apply {
                this.currentYear = year
                this.currentMonth = month
                this.eventDateList = events
                postInvalidate() // Redraw the custom view if needed
                configureCustomViewCallback?.invoke(this) // Optional callback for further customization
            }
        }
    }

    private var configureCustomViewCallback: ((CustomViewMonth) -> Unit)? = null
    fun configureCustomView(callback: (CustomViewMonth) -> Unit) {
        this.configureCustomViewCallback = callback
    }
}



