package calendar.schedule.task.todo.event.reminder.presentation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import calendar.schedule.task.todo.event.reminder.databinding.ItemYearPage1Binding
import calendar.schedule.task.todo.event.reminder.presentation.ui.custom_view.CustomViewMonth

class CalendarYearPageAdapter() :
    RecyclerView.Adapter<CalendarYearPageAdapter.MonthViewHolder>() {

    private var yearList: Map<Int, Map<Int, List<Int>>> = mutableMapOf()
    // Method to update yearList and refresh the RecyclerView
    @SuppressLint("NotifyDataSetChanged")
    fun updateYearList(newYearList: Map<Int, Map<Int, List<Int>>>) {
        yearList = newYearList
        notifyDataSetChanged()
    }

    private var firstDayOfTheWeek = "Sunday"
    @SuppressLint("NotifyDataSetChanged")
    fun updateFirstDayOfTheWeek(firstDay: String) {
        firstDayOfTheWeek = firstDay
        notifyDataSetChanged()
    }

    private var selectedDate: String? = null
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedDate(yyyy_mm_dd: String?) {
        if (selectedDate != yyyy_mm_dd) {
            selectedDate = yyyy_mm_dd
            notifyDataSetChanged()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemYearPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val yearKey: Int = yearList.keys.toList()[position]

        val weekStart = when(firstDayOfTheWeek){
            "Sunday" -> CustomViewMonth.WeekStart.SUNDAY
            "Monday" -> CustomViewMonth.WeekStart.MONDAY
            else -> { CustomViewMonth.WeekStart.SATURDAY }
        }
        holder.binding.apply {
            customViewMonth1.apply { currentYear = yearKey ; currentMonth = 0 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth2.apply { currentYear = yearKey ; currentMonth = 1 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth3.apply { currentYear = yearKey ; currentMonth = 2 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth4.apply { currentYear = yearKey ; currentMonth = 3 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth5.apply { currentYear = yearKey ; currentMonth = 4 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth6.apply { currentYear = yearKey ; currentMonth = 5 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth7.apply { currentYear = yearKey ; currentMonth = 6 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth8.apply { currentYear = yearKey ; currentMonth = 7 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth9.apply { currentYear = yearKey ; currentMonth = 8 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth10.apply { currentYear = yearKey ; currentMonth = 9 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth11.apply { currentYear = yearKey ; currentMonth = 10 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth12.apply { currentYear = yearKey ; currentMonth = 11 ; weekStart(weekStart) ; this.selectedDate = this@CalendarYearPageAdapter.selectedDate ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }

        }
    }

    override fun getItemCount(): Int = yearList.size

    inner class MonthViewHolder(val binding: ItemYearPage1Binding) : RecyclerView.ViewHolder(binding.root)

    private var getYearMonth: ((Int, Int) -> Unit)? = null
    fun getYearMonth(block: (Int, Int) -> Unit) {
        getYearMonth = block
    }
}



