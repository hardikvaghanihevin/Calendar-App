package calendar.schedule.task.todo.event.reminder.presentation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import calendar.schedule.task.todo.event.reminder.common.Constants.BASE_TAG
import calendar.schedule.task.todo.event.reminder.data.database.entity.DayKey
import calendar.schedule.task.todo.event.reminder.data.database.entity.EventValue
import calendar.schedule.task.todo.event.reminder.data.database.entity.MonthKey
import calendar.schedule.task.todo.event.reminder.data.database.entity.YearKey
import calendar.schedule.task.todo.event.reminder.databinding.ItemMonthPage1Binding
import calendar.schedule.task.todo.event.reminder.presentation.ui.custom_view.CustomViewMonth

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
        this.yearMonthPairList = newYearMonthPairList
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
            }
            configureCustomViewCallback?.invoke(cvm) // Optional callback for further customization
        }
    }

    override fun getItemCount(): Int = yearMonthPairList.size

    inner class MonthViewHolder(val binding: ItemMonthPage1Binding) : RecyclerView.ViewHolder(binding.root)

    private var configureCustomViewCallback: ((CustomViewMonth) -> Unit)? = null
    fun configureCustomView(callback: (CustomViewMonth) -> Unit) {
        this.configureCustomViewCallback = callback
    }
}



