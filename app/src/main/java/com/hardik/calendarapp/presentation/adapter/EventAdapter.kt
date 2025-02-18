package com.hardik.calendarapp.presentation.adapter

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.ItemEventLayout1Binding
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.DATE_FORMAT_dd_MM_yyyy_1
import com.hardik.calendarapp.utillities.DateUtil.DATE_FORMAT_yyyy_MM_dd
import com.hardik.calendarapp.utillities.DateUtil.TIME_FORMAT_HH_mm
import com.hardik.calendarapp.utillities.DateUtil.isAllDay
import com.hardik.calendarapp.utillities.ImageColorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

class EventAdapter(): RecyclerView.Adapter<EventAdapter.ViewHolder>(), Filterable {
    private val TAG = BASE_TAG + EventAdapter::class.java.simpleName

    // To handle concurrent updates
    private val updateMutex = Mutex()
    private var updateJob: Job? = null
    private var latestData: Pair<List<Event>, Map<String, Event>>? = null

    // Keep a copy of the original list for filtering
    private var originalList: MutableList<Event> = arrayListOf() // Keep an immutable copy
    private var filteredList: List<Event> = originalList

    private var firstEventOfEachWeek: Map<String, Event> = mutableMapOf()
    @SuppressLint("NotifyDataSetChanged")
    fun updateFirstEventOfWeek(fEEW : Map<String, Event>){
        firstEventOfEachWeek = fEEW
    }
    fun isEventPresent(event: Event): Boolean {
        return firstEventOfEachWeek.containsValue(event)
    }

    @SuppressLint("NotifyDataSetChanged")
    suspend fun updateData(newData: List<Event>, fEEW: Map<String, Event>) {
        latestData = newData to fEEW

        updateMutex.withLock { // Ensure sequential processing
            updateJob?.cancel()
            updateJob = CoroutineScope(Dispatchers.Main).launch {
                val (finalNewData, finalFEEW) = latestData ?: return@launch
                updateFirstEventOfWeek(fEEW = finalFEEW)

                // Create copies of the lists *before* starting DiffUtil
                val oldListCopy = originalList.toList() // Important: Create a copy
                val newListCopy = finalNewData.toList()     // Important: Create a copy

                withContext(Dispatchers.Default) {
                    val diffCallback = object : DiffUtil.Callback() {
                        override fun getOldListSize() = oldListCopy.size // Use the copy
                        override fun getNewListSize() = newListCopy.size // Use the copy

                        override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                        ): Boolean {
                            if (oldItemPosition >= oldListCopy.size || newItemPosition >= newListCopy.size) return false
                            return oldListCopy[oldItemPosition].id == newListCopy[newItemPosition].id
                        }

                        override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                        ): Boolean {
                            if (oldItemPosition >= oldListCopy.size || newItemPosition >= newListCopy.size) return false
                            // Compare all properties to check if the contents are the same
                            return oldListCopy[oldItemPosition].id == newListCopy[newItemPosition].id &&
                                    oldListCopy[oldItemPosition].title == newListCopy[newItemPosition].title &&
                                    oldListCopy[oldItemPosition].startTime == newListCopy[newItemPosition].startTime &&
                                    oldListCopy[oldItemPosition].endTime == newListCopy[newItemPosition].endTime &&
                                    oldListCopy[oldItemPosition].startDate == newListCopy[newItemPosition].startDate &&
                                    oldListCopy[oldItemPosition].endDate == newListCopy[newItemPosition].endDate &&
                                    oldListCopy[oldItemPosition].year == newListCopy[newItemPosition].year &&
                                    oldListCopy[oldItemPosition].month == newListCopy[newItemPosition].month &&
                                    oldListCopy[oldItemPosition].date == newListCopy[newItemPosition].date &&
                                    oldListCopy[oldItemPosition].isHoliday == newListCopy[newItemPosition].isHoliday &&
                                    oldListCopy[oldItemPosition].eventType == newListCopy[newItemPosition].eventType &&
                                    oldListCopy[oldItemPosition].sourceType == newListCopy[newItemPosition].sourceType &&
                                    oldListCopy[oldItemPosition].description == newListCopy[newItemPosition].description &&
                                    oldListCopy[oldItemPosition].repeatOption == newListCopy[newItemPosition].repeatOption &&
                                    oldListCopy[oldItemPosition].alertOffset == newListCopy[newItemPosition].alertOffset &&
                                    oldListCopy[oldItemPosition].customAlertOffset == newListCopy[newItemPosition].customAlertOffset &&
                                    oldListCopy[oldItemPosition].triggerTime == newListCopy[newItemPosition].triggerTime

                        }

                    }

                    val diffResult = DiffUtil.calculateDiff(diffCallback)

                    withContext(Dispatchers.Main) {
                        originalList = newListCopy.toMutableList() // Update with the *new* list
                        setFirstEventOfEachWeek(originalList)
                        filteredList = originalList // Update filtered list
                        notifyDataSetChanged()
                        diffResult.dispatchUpdatesTo(this@EventAdapter)
                    }
                }

            }
        }


    }

    var weekStart = Calendar.SUNDAY
    @SuppressLint("NotifyDataSetChanged")
    fun updateFirstDayOfWeek(weekStart: Int = Calendar.SUNDAY) {
        this.weekStart = weekStart
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventLayout1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filteredList.size // Use filtered list for display
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Pass the previous event to check if the current one is on the same date
        if (position in filteredList.indices) {
            val previousEvent = if (position > 0) filteredList[position - 1] else null
            val isMonthView = firstDatesByMonthMap.size == 1
            holder.bind(filteredList[position], previousEvent, position,isMonthView = isMonthView)
        } else {
            //Log.e(TAG, "Invalid position: $position")
        }

    }
    inner class ViewHolder(private val binding: ItemEventLayout1Binding) :
        RecyclerView.ViewHolder(binding.root) {

        private val layout = binding.root
        private val scope = CoroutineScope(Dispatchers.Main) // Create a CoroutineScope for this ViewHolder

        fun clear() {
            // Cancel the CoroutineScope to clean up resources when ViewHolder is recycled
            //scope.coroutineContext.cancelChildren()
            scope.cancel()
        }

        @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
        fun bind(event: Event, previousEvent: Event?, position: Int, isMonthView: Boolean = false) {
            binding.apply {
                // Check if the current event's month is different from the previous event
                // ===== Month header code (unchanged) =====
                val currentMonth = DateUtil.getMonthName(event.startDate, DATE_FORMAT_yyyy_MM_dd)
                val previousMonth = previousEvent?.let { DateUtil.getMonthName(it.startDate, DATE_FORMAT_yyyy_MM_dd) }

                    // Show divider image when the month changes
                if (firstDatesByMonthMap.containsKey(event.startDate) && firstDatesByMonthMap.containsValue(
                        Pair(event.id, true)
                    ) && !isMonthView) {
                    cardItemEventImg.visibility = View.VISIBLE // Show the CardView
                    imgItemEventLayMonthTransitionImage.visibility = View.VISIBLE // Show the image
                    tvItemEventMonthName.text = "$currentMonth ${event.year}"

                    llItemEvent.visibility = View.VISIBLE// Show week range after image (need)

                    val imageUrl = ImageColorUtil.monthImgResource.get(event.month.toInt())

                    Glide.with(imgItemEventLayMonthTransitionImage.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bkg_01_jan)
                        .error(R.drawable.bkg_01_jan)
                        .into(imgItemEventLayMonthTransitionImage)
                } else {
                    cardItemEventImg.visibility = View.GONE
                    imgItemEventLayMonthTransitionImage.visibility = View.GONE
                }
                // ===== End Month header code =====


                // ===== Week header logic =====
                val dateForWeek = DateUtil.stringToString( dateString = event.startDate,
                    inputPattern = DATE_FORMAT_yyyy_MM_dd,
                    outputPattern = DATE_FORMAT_dd_MM_yyyy_1)
                val weekRange = DateUtil.getWeekRange(dateForWeek, weekStart)
                val weekStart = weekRange.second
                val weekEnd = weekRange.third
                eventFullWeekDate.text = weekRange.first

                // Show week header (llItemEvent) only for the first event of a week
                val isMatchFound = firstEventOfEachWeek.keys.any {
                    it.lowercase(Locale.getDefault()).contains("${event.year}-${event.month}")
                }
                if (isEventPresent(event) && isMatchFound) {
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
                eventTimePeriod.text = if (isAllDay(startTime = event.startTime, endTime = event.endTime))
                    ContextCompat.getString(binding.root.context, R.string.all_day)
                else {
                    val startTime = DateUtil.longToString(event.startTime, TIME_FORMAT_HH_mm)
                    val endTime = DateUtil.longToString(event.endTime, TIME_FORMAT_HH_mm)
                    if (startTime == "00:00" && endTime == "00:00") "-"
                    else if (startTime == "00:00" && endTime == "23:59") ContextCompat.getString(binding.root.context, R.string.all_day)
                    else "$startTime (${DateUtil.getDuration(startTimestamp = event.startTime, endTimestamp = DateUtil.mergeDateAndTime(DateUtil.stringToLong(event.endDate), event.endTime))})" //- $endTime"
                }

                // Handle item clicks
                itemEventLayout.setOnClickListener { configureEventCallBack?.invoke(event) }

            }

        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.clear() // Cancel ongoing coroutines in the ViewHolder
    }

    private var configureEventCallBack: ((event: Event) -> Unit)? = null
    fun setConfigureEventCallback(callback: (event: Event) -> Unit) {
        configureEventCallBack = callback
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
                setFirstEventOfEachWeek(filteredList)

                noDataCallback?.invoke(true.takeUnless { filteredList.isEmpty() } ?: false) // Notify when no data is found

                notifyDataSetChanged()
            }
        }
    }

    private var noDataCallback: ((hasData: Boolean) -> Unit)? = null

    fun setNoDataCallback(callback: (Boolean) -> Unit) {
        noDataCallback = callback
    }

    private var firstDatesByMonthMap: MutableMap<String, Pair<String, Boolean>> = mutableMapOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setFirstEventOfEachWeek(newData: List<Event>) {
        // Clear previous data
        this.firstDatesByMonthMap.clear()

        val tempMap: Map<String, Pair<String, Boolean>> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sortedDates = newData.map {
                // Parse startDate into LocalDate and associate it with the Event ID
                LocalDate.parse(it.startDate) to it
            }.sortedBy { it.first }  // Sort by the date (LocalDate)

            sortedDates.groupBy { it.first.year to it.first.month } // Group by Year-Month
                .map { (_, dates) ->
                    // Get the first event of each group
                    val firstEvent = dates.first().second // Access the Event object
                    firstEvent.startDate to Pair(firstEvent.id, true) // Use Event ID and mark as true
                }
                .toMap() // Convert to a Map
        } else {
            val dateFormat = SimpleDateFormat(DATE_FORMAT_yyyy_MM_dd, Locale.getDefault())
            val sortedDates = newData.map {
                // Parse startDate and associate it with the Event ID
                dateFormat.parse(it.startDate) to it
            }.sortedBy { it.first } // Sort by the Date

            sortedDates.groupBy { date ->
                // Group by Year-Month in the format "yyyy-MM"
                val calendar = Calendar.getInstance().apply { time = date.first }
                "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
            }
                .map { (_, dates) ->
                    // Get the first event of each group
                    val firstEvent = dates.first().second // Access the Event object
                    dateFormat.format(firstEvent.startDate) to Pair(firstEvent.id, true) // Use Event ID and mark as true
                }
                .toMap() // Convert to a Map
        }


        // Update the mutable map
        this.firstDatesByMonthMap.putAll(tempMap)
        notifyDataSetChanged()
    

        val dateFormatter: DateTimeFormatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern(DATE_FORMAT_yyyy_MM_dd)
        } else {
            //SimpleDateFormat(DATE_FORMAT_yyyy_MM_dd, Locale.getDefault())
            TODO("VERSION.SDK_INT < O")
        }

        val firstDayOfWeek = when (weekStart) {
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }

        val groupedByYearWeek = newData.groupBy { event ->
            val localDate = LocalDate.parse(event.startDate, dateFormatter)
            val weekField = WeekFields.of(firstDayOfWeek, 1).weekOfWeekBasedYear()
            val year = localDate.getYear()
            val month = localDate.monthValue - 1  // Convert to 0-based month
            val week = localDate.get(weekField)
            "$year-$month-$week"  // Unique key for year-week combination
        }

        val firstEvents = groupedByYearWeek.mapValues { (key, events) ->
            events.minByOrNull { LocalDate.parse(it.startDate, dateFormatter) }!!
        }

        // Update StateFlow with new values
        firstEventOfEachWeek = firstEvents.mapKeys { it.key }
    }
}
