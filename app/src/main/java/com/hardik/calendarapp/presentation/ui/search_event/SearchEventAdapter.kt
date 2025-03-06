package com.hardik.calendarapp.presentation.ui.search_event

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.ItemEventLayout1Binding
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.ImageColorUtil
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

class SearchEventAdapter(private val context: Context) : ListAdapter<Event, SearchEventAdapter.ViewHolder>(EventDiffCallback()) {
    private val TAG = BASE_TAG + SearchEventAdapter::class.java.simpleName
    private var configureEventCallback: ((event: Event) -> Unit)? = null

    private var weekStart: Int = Calendar.SUNDAY
    private var firstEventOfEachWeek: Map<String, Event> = mutableMapOf()//@
    private var firstDatesByMonthMap: Map<String, Pair<String, Boolean>> = mutableMapOf()//@

    override fun submitList(list: List<Event>?) {
        super.submitList(list)
        list?.let {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            weekStart = when (sharedPreferences.getString("firstDayOfWeek", "Sunday")) {// Default to Sunday
                "Sunday" -> Calendar.SUNDAY
                "Monday" -> Calendar.MONDAY
                "Saturday" -> Calendar.SATURDAY
                else -> { Calendar.SUNDAY }
            }
            val dataUpdate = updateEventData(it, weekStart)
            weekStart = dataUpdate.first
            firstEventOfEachWeek = dataUpdate.second
            firstDatesByMonthMap = dataUpdate.third
        }
    }

    //fun isEventPresent(event: Event): Boolean { return firstEventOfEachWeek.containsValue(event) }


    fun setConfigureEventCallback(callback: (event: Event) -> Unit) {
        configureEventCallback = callback
    }

    inner class ViewHolder(private val binding: ItemEventLayout1Binding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: Event, previousEvent: Event?, position: Int, isMonthView: Boolean = false) {
            binding.apply {
                // Check if the current event's month is different from the previous event
                // ===== Month header code (unchanged) =====
                val currentMonth = DateUtil.getMonthName(event.startDate,
                    DateUtil.DATE_FORMAT_yyyy_MM_dd
                )
                val previousMonth = previousEvent?.let { DateUtil.getMonthName(it.startDate,
                    DateUtil.DATE_FORMAT_yyyy_MM_dd
                ) }

                var isImageShown = false
                // Show divider image when the month changes
                if (firstDatesByMonthMap.containsKey(event.startDate) && firstDatesByMonthMap.containsValue(Pair(event.id, true)) && !isMonthView) {
                    cardItemEventImg.visibility = View.VISIBLE // Show the CardView
                    imgItemEventLayMonthTransitionImage.visibility = View.VISIBLE // Show the image
                    tvItemEventMonthName.text = "$currentMonth ${event.year}"

                    llItemEvent.visibility = View.VISIBLE// Show week range after image (need)
                    isImageShown = true

                    val imageUrl = ImageColorUtil.monthImgResource.get(event.month.toInt())

                    Glide.with(imgItemEventLayMonthTransitionImage.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bkg_01_jan)
                        .error(R.drawable.bkg_01_jan)
                        .into(imgItemEventLayMonthTransitionImage)
                } else {
                    cardItemEventImg.visibility = View.GONE
                    imgItemEventLayMonthTransitionImage.visibility = View.GONE
                    isImageShown = false
                }
                // ===== End Month header code =====


                // ===== Week header logic =====
                val dateForWeek = DateUtil.stringToString( dateString = event.startDate,
                    inputPattern = DateUtil.DATE_FORMAT_yyyy_MM_dd,
                    outputPattern = DateUtil.DATE_FORMAT_dd_MM_yyyy_1
                )
                val weekRange = DateUtil.getWeekRange(dateForWeek, weekStart)
                val weekStart = weekRange.second
                val weekEnd = weekRange.third
                eventFullWeekDate.text = weekRange.first

                // Show week header (llItemEvent) only for the first event of a week
                val isMatchFound = firstEventOfEachWeek.keys.any {
                    it.lowercase(Locale.getDefault()).contains("${event.year}-${event.month}")
                }

                val isEventPresent = firstEventOfEachWeek.containsValue(event)

                if (isEventPresent || isImageShown) {
                    llItemEvent.visibility = View.VISIBLE
                } else {
                    llItemEvent.visibility = View.GONE
                }
                // ===== End Week header logic =====




                // ===== Date header logic for individual dates =====
                // Only show the date if the previous event has a different startDate
                if (previousEvent != null && previousEvent.startDate == event.startDate) {
                    eventDayDate.visibility = View.INVISIBLE
                } else {
                    eventDayDate.visibility = View.VISIBLE
                    eventDayDate.text = "${DateUtil.getDayName(event.startDate, isShort = true)}\n${event.date}" // e.g., Wed, 1
                }
                // ===== End Date header logic =====

                // Set event title
                eventTitle.text = event.title

                // Set "All day" or time period based on start and end time
                eventTimePeriod.text = if (event.isAllDay){
                    ContextCompat.getString(binding.root.context, R.string.all_day)
                } else{
                    //if (isAllDay(startTime = event.startTime, endTime = event.endTime)) ContextCompat.getString(binding.root.context, R.string.all_day)
                    //else {
                    val startTime = DateUtil.longToString(event.startTime,
                        DateUtil.TIME_FORMAT_HH_mm
                    )
                    val endTime = DateUtil.longToString(event.endTime, DateUtil.TIME_FORMAT_HH_mm)
                    //if (startTime == "00:00" && endTime == "00:00") "-"
                    //else if (startTime == "00:00" && endTime == "23:59") ContextCompat.getString(binding.root.context, R.string.all_day)else
                    "$startTime (${DateUtil.getDuration(startTimestamp = event.startTime, endTimestamp = DateUtil.mergeDateAndTime(DateUtil.stringToLong(event.endDate), event.endTime))})" //- $endTime"
                    //}
                }

                // Handle item clicks
                constLay1ItemEvent.setOnClickListener { configureEventCallback?.invoke(event) }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventLayout1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position in currentList.indices) {
            val previousEvent = if (position > 0) currentList[position - 1] else null
            val isMonthView = currentList.size == 1
            holder.bind(currentList[position], previousEvent, position, isMonthView)
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        //return oldItem == newItem
        return oldItem.id == newItem.id &&
                oldItem.title == newItem.title &&
                oldItem.startTime == newItem.startTime &&
                oldItem.endTime == newItem.endTime &&
                oldItem.startDate == newItem.startDate &&
                oldItem.endDate == newItem.endDate &&
                oldItem.year == newItem.year &&
                oldItem.month == newItem.month &&
                oldItem.date == newItem.date &&
                oldItem.isHoliday == newItem.isHoliday &&
                oldItem.eventType == newItem.eventType &&
                oldItem.sourceType == newItem.sourceType &&
                oldItem.description == newItem.description &&
                oldItem.repeatOption == newItem.repeatOption &&
                oldItem.alertOffset == newItem.alertOffset &&
                oldItem.customAlertOffset == newItem.customAlertOffset &&
                oldItem.triggerTime == newItem.triggerTime

    }
}


fun updateEventData(newData: List<Event>, weekStart: Int): Triple<Int, Map<String, Event>, Map<String, Pair<String, Boolean>>> {
    if (newData.isEmpty()) { return Triple(weekStart, emptyMap(), emptyMap()) }

    val firstDatesByMonthMap = mutableMapOf<String, Pair<String, Boolean>>()
    val firstEventOfEachWeek = mutableMapOf<String, Event>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val dateFormatter = DateTimeFormatter.ofPattern(DateUtil.DATE_FORMAT_yyyy_MM_dd)

        // Process first event of each month
        newData.map { LocalDate.parse(it.startDate, dateFormatter) to it }
            .sortedBy { it.first }
            .groupBy { it.first.year to it.first.monthValue } // ✅ Fixed incorrect month grouping
            .forEach { (_, dates) ->
                val firstEvent = dates.first().second
                firstDatesByMonthMap[firstEvent.startDate] = Pair(firstEvent.id, true)
            }

        // Determine first event of each week
        val firstDayOfWeek = when (weekStart) {
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }

        val groupedByYearWeek = newData.groupBy { event ->
            val localDate = LocalDate.parse(event.startDate, dateFormatter)
            val weekField = WeekFields.of(firstDayOfWeek, 1).weekOfWeekBasedYear()
            val year = localDate.year
            val week = localDate.get(weekField)
            "$year-$week" // ✅ Fixed incorrect week calculation
        }

        groupedByYearWeek.forEach { (_, events) ->
            firstEventOfEachWeek[events.first().startDate] = events.first()
        }

    } else {
        val dateFormat = SimpleDateFormat(DateUtil.DATE_FORMAT_yyyy_MM_dd, Locale.getDefault())

        // Process first event of each month
        newData.mapNotNull { event ->
            dateFormat.parse(event.startDate)?.let { it to event }
        }.sortedBy { it.first }
            .groupBy {
                val calendar = Calendar.getInstance().apply { time = it.first }
                "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
            }
            .forEach { (_, dates) ->
                val firstEvent = dates.first().second
                firstDatesByMonthMap[firstEvent.startDate] = Pair(firstEvent.id, true)
            }

        // Determine first event of each week
        val calendar = Calendar.getInstance()
        val groupedByYearWeek = newData.groupBy { event ->
            dateFormat.parse(event.startDate)?.let { date ->
                calendar.time = date
                val year = calendar.get(Calendar.YEAR)
                val week = calendar.get(Calendar.WEEK_OF_YEAR)
                "$year-$week"
            } ?: ""
        }

        groupedByYearWeek.forEach { (_, events) ->
            events.firstOrNull()?.let {
                firstEventOfEachWeek[it.startDate] = it
            }
        }
    }

    return Triple(weekStart, firstEventOfEachWeek, firstDatesByMonthMap)
}


