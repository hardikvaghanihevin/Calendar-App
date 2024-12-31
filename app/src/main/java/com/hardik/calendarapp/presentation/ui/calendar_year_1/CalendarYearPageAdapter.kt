package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.R
import com.hardik.calendarapp.databinding.ItemYearPage1Binding
import com.hardik.calendarapp.presentation.ui.custom_view.CustomViewMonth

class CalendarYearPageAdapter(private var yearList: Map<Int, Map<Int, List<Int>>>) :
    RecyclerView.Adapter<CalendarYearPageAdapter.MonthViewHolder>() {

    private var yr:Int = 0
    @SuppressLint("NotifyDataSetChanged")
    fun updateYear(year: Int) {
        yr = year
        notifyItemChanged(year)
        Log.i("TAG", "updateYear: $yr")
    }

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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemYearPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val context = holder.itemView.context
        val yearKey: Int = yearList.keys.toList()[position]

//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//        val firstDayOfTheWeek = sharedPreferences.getString("firstDayOfWeek", "Sunday")// Default to Sunday

        val weekStart = when(firstDayOfTheWeek){
            "Sunday" -> CustomViewMonth.WeekStart.SUNDAY
            "Monday" -> CustomViewMonth.WeekStart.MONDAY
            else -> { CustomViewMonth.WeekStart.SATURDAY }
        }
        holder.binding.apply {
            val bgDayColor = ContextCompat.getColor(context, R.color.blue)
            customViewMonth1.apply { currentYear = yearKey ; currentMonth = 0 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth2.apply { currentYear = yearKey ; currentMonth = 1 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth3.apply { currentYear = yearKey ; currentMonth = 2 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth4.apply { currentYear = yearKey ; currentMonth = 3 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth5.apply { currentYear = yearKey ; currentMonth = 4 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth6.apply { currentYear = yearKey ; currentMonth = 5 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth7.apply { currentYear = yearKey ; currentMonth = 6 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth8.apply { currentYear = yearKey ; currentMonth = 7 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth9.apply { currentYear = yearKey ; currentMonth = 8 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth10.apply { currentYear = yearKey ; currentMonth = 9 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth11.apply { currentYear = yearKey ; currentMonth = 10 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }
            customViewMonth12.apply { currentYear = yearKey ; currentMonth = 11 ; weekStart(weekStart) ; setOnClickListener { getYearMonth?.invoke(currentYear, currentMonth) } }

        }
    }

    override fun getItemCount(): Int = yearList.size

    inner class MonthViewHolder(val binding: ItemYearPage1Binding) : RecyclerView.ViewHolder(binding.root)

    private var getYearMonth: ((Int, Int) -> Unit)? = null
    fun getYearMonth(block: (Int, Int) -> Unit) {
        getYearMonth = block
    }
}



