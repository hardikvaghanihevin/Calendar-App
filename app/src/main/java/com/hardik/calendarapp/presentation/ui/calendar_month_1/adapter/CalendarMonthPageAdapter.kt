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

class CalendarMonthPageAdapter(private val yearMonthPairList : List<Pair<Int, Int>>) :
    RecyclerView.Adapter<CalendarMonthPageAdapter.MonthViewHolder>() {
    private val TAG = BASE_TAG + CalendarMonthPageAdapter::class.java.simpleName

    private var eventsOfDateMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()
    @SuppressLint("NotifyDataSetChanged")
    fun updateEventsOfDate(dates: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>) {
        this.eventsOfDateMap = dates
        notifyDataSetChanged()
    }

    private var selectedDate:String? = null
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedDate(yyyy_mm_dd: String?){
        this.selectedDate = yyyy_mm_dd
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        //val monthIndex = position % 12
        val (year, month) = yearMonthPairList[position]

        holder.binding.customView.apply {
            this.currentYear = year
            this.currentMonth = month
            this@CalendarMonthPageAdapter.selectedDate?.let { this.selectedDate = it }//"2024-11-25"
            enableTouchEventHandling(enable = true)
            this.eventDateList = eventsOfDateMap
            postInvalidate() // Redraw the custom view if needed
            configureCustomViewCallback?.invoke(this) // Optional callback for further customization
        }
        //holder.bind(eventsOfDateMap)
    }

    override fun getItemCount(): Int = yearMonthPairList.size

    inner class MonthViewHolder(val binding: ItemMonthPage1Binding) :
        RecyclerView.ViewHolder(binding.root) {
        /*fun bind(eventMap: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()) {
            binding.apply {
                customView.apply {
                    this.currentYear = 2024
                    this.currentMonth = 11
                    enableTouchEventHandling(enable = true)
                    this.eventDateList = this@CalendarMonthPageAdapter.eventsOfDateMap
                    postInvalidate() // Redraw the custom view if needed
                    configureCustomViewCallback?.invoke(this) // Optional callback for further customization
                }
            }
        }*/
    }

    private var configureCustomViewCallback: ((CustomViewMonth) -> Unit)? = null
    fun configureCustomView(callback: (CustomViewMonth) -> Unit) {
        this.configureCustomViewCallback = callback
    }
    private var getYearMonth: ((Int) -> Unit)? = null
    fun getYearMonth(block: (Int) -> Unit) {
        getYearMonth = block
    }
}



