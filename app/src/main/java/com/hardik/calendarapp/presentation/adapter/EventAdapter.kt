package com.hardik.calendarapp.presentation.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.ItemEventLayout1Binding
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.DATE_FORMAT_yyyy_MM_dd
import com.hardik.calendarapp.utillities.DateUtil.isAllDay
import com.hardik.calendarapp.utillities.GsonUtil
import com.hardik.calendarapp.utillities.LogUtil
import java.util.Calendar
import java.util.Date
import java.util.Locale

//class EventAdapter(private var list: ArrayList<CalendarDetail.Item>, private val dateItemClickListener: DateItemClickListener):
class EventAdapter(private var list: ArrayList<Event>): RecyclerView.Adapter<EventAdapter.ViewHolder>(), Filterable {
    private val TAG = BASE_TAG + EventAdapter::class.java.simpleName

    // Keep a copy of the original list for filtering
    private var originalList: List<Event> = list.toList() // Keep an immutable copy
    private var filteredList: List<Event> = originalList

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Event>) {
        list.clear()
        list.addAll(newData)
        originalList = newData.toList() // Update the original list
        filteredList = originalList
        notifyDataSetChanged()

        val eventJson = GsonUtil.toJson(list)
        LogUtil.logLongMessage(TAG, "onViewCreated: $eventJson")

        Log.e(TAG, "updateData: ${list.size}")
    }


    //region Optional: If you only need to change a small set of data, you can use more specific methods
    //endregion
    fun updateItem(position: Int, newItem: Event) {
        list[position] = newItem
        notifyItemChanged(position)
    }

    var weekStart = Calendar.SUNDAY
    @SuppressLint("NotifyDataSetChanged")
    fun updateFirstDayOfWeek(weekStart: Int = Calendar.SUNDAY) {
        this.weekStart = weekStart
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventLayout1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
//        val lp = RecyclerView.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//        )
//        binding.root.apply { layoutParams = lp }
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filteredList.size // Use filtered list for display
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Pass the previous event to check if the current one is on the same date
        val previousEvent = if (position > 0) filteredList[position - 1] else null
        holder.bind( filteredList[position], previousEvent, position )
    }

    inner class ViewHolder(private val binding: ItemEventLayout1Binding) :
        RecyclerView.ViewHolder(binding.root) {

        private val layout = binding.root

        @SuppressLint("SetTextI18n")
        fun bind(event: Event, previousEvent: Event?, position: Int) {
            binding.apply {
                // Check if the current event's month is different from the previous event
                val currentMonth = DateUtil.getMonthName(event.startDate, DATE_FORMAT_yyyy_MM_dd)
                val previousMonth = previousEvent?.let { DateUtil.getMonthName(it.startDate, DATE_FORMAT_yyyy_MM_dd) }

                // Show divider image when the month changes
                if (previousMonth != null && previousMonth != currentMonth) {
                    monthTransitionImage.visibility = View.VISIBLE // Show the image
                } else {
                    monthTransitionImage.visibility = View.GONE // Hide the image
                }

                // Check if the current event's week is the same as the previous event
                val currentEventWeek = DateUtil.getWeekOfYear(event.startDate, DATE_FORMAT_yyyy_MM_dd)
                val previousEventWeek = previousEvent?.let { DateUtil.getWeekOfYear(it.startDate, DATE_FORMAT_yyyy_MM_dd) }

                // Check if the current event's date is the same as the previous event
                if (previousEvent != null && previousEvent.startDate == event.startDate) {
                    // Hide day name and date for duplicate dates
                    eventDayDate.visibility = View.INVISIBLE
                    eventFullWeekDate.visibility = View.GONE
                } else {
                    // Show day name and date
                    eventDayDate.visibility = View.VISIBLE
                    eventDayDate.text = "${DateUtil.getDayName(event.startDate, isShort = true)}\n${event.date}" // e.g., Wed, 1
                    //eventFullWeekDate.visibility = View.VISIBLE
                    // Display the week range for the first event of each week
                    if (previousEventWeek != currentEventWeek) {

                        // Set week range if applicable
                        // Display the full week range header for the first event of the week
                        val dateForWeek = "${event.date}-${event.month.toInt().plus(1)}-${event.year}"
                        eventFullWeekDate.visibility = View.VISIBLE
                        eventFullWeekDate.text = DateUtil.getWeekRange(dateForWeek, weekStart)

                    } else {
                        eventFullWeekDate.visibility = View.GONE
                    }
                }

                // Set event title
                eventTitle.text = event.title

                // Set "All day" or time period based on start and end time
                eventTimePeriod.text = if ( isAllDay(startTime = event.startTime, endTime = event.endTime) ) "All day"
                else "${DateUtil.longToString(event.startTime, "HH:mm")} - ${DateUtil.longToString(event.endTime, "HH:mm")}"
                //eventTimePeriod.text = DateUtil.longToString(event.endTime, DateUtil.DATE_FORMAT_yyyy_MM_dd)

                // Handle item clicks
                itemEventLayout.setOnClickListener { configureEventCallBack?.invoke(event) }
            }
        }
    }

    private var configureEventCallBack: ((event: Event) -> Unit)? = null
    fun setConfigureEventCallback(callback: (event: Event) -> Unit) {
        configureEventCallBack = callback
    }

    private fun formatTime(dateTime: String?): String {
        if (dateTime == null) return ""
        val date: Date =
            DateUtil.stringToDate(dateTime, DateUtil.DATE_TIME_FORMAT_yyyy_MM_dd_T_HH_MM_ss_Z)
                ?: Date()
        return DateUtil.dateToString(date, DateUtil.DATE_TIME_FORMAT_yyyy_MM_dd_HH_mm)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase(Locale.getDefault()).orEmpty()
                val results = if (query.isEmpty()) {
                    originalList
                } else {
                    originalList.filter {
                        it.title.lowercase(Locale.getDefault()).contains(query) ||
                                it.startDate.contains(query)
                    }
                }
                return FilterResults().apply { values = results }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as? List<Event> ?: originalList
                notifyDataSetChanged()
                Log.d(TAG, "Filtered list size: ${filteredList.size}")
            }
        }
    }

}
