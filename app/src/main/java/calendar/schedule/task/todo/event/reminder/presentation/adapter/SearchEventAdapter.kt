package calendar.schedule.task.todo.event.reminder.presentation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import calendar.schedule.task.todo.event.reminder.R
import calendar.schedule.task.todo.event.reminder.common.Constants.BASE_TAG
import calendar.schedule.task.todo.event.reminder.common.Constants.PREF_KEY_FIRST_DAY_OF_THE_WEEK
import calendar.schedule.task.todo.event.reminder.data.database.entity.Event
import calendar.schedule.task.todo.event.reminder.databinding.ItemEventLayout1Binding
import calendar.schedule.task.todo.event.reminder.presentation.adapter.diff_util.EventDiffCallback
import calendar.schedule.task.todo.event.reminder.utillities.DateUtil
import calendar.schedule.task.todo.event.reminder.utillities.EventListHelper
import calendar.schedule.task.todo.event.reminder.utillities.ImageColorUtil
import java.util.Calendar

class SearchEventAdapter(private val context: Context) : ListAdapter<Event, SearchEventAdapter.ViewHolder>(
    EventDiffCallback()
) {
    private val TAG = BASE_TAG + SearchEventAdapter::class.java.simpleName
    private var configureEventCallback: ((event: Event) -> Unit)? = null

    private var weekStart: Int = Calendar.SUNDAY
    private var firstEventOfEachWeek: Map<String, Event> = mutableMapOf()//@
    private var firstDatesByMonthMap: Map<String, Pair<String, Boolean>> = mutableMapOf()//@

    override fun submitList(list: List<Event>?) {
        super.submitList(list)
        list?.let {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            weekStart = when (sharedPreferences.getString(PREF_KEY_FIRST_DAY_OF_THE_WEEK, "Sunday")) {// Default to Sunday
                "Sunday" -> Calendar.SUNDAY
                "Monday" -> Calendar.MONDAY
                "Saturday" -> Calendar.SATURDAY
                else -> { Calendar.SUNDAY }
            }
            val dataUpdate = EventListHelper().getEventListHelperData(it, weekStart)
            weekStart = dataUpdate.first
            firstEventOfEachWeek = dataUpdate.second
            firstDatesByMonthMap = dataUpdate.third
        }
    }

    fun setConfigureEventCallback(callback: (event: Event) -> Unit) {
        configureEventCallback = callback
    }

    inner class ViewHolder(private val binding: ItemEventLayout1Binding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(event: Event, previousEvent: Event?, position: Int, isMonthView: Boolean = false) {
            binding.apply {
                // Check if the current event's month is different from the previous event
                // ===== Month header code (unchanged) =====
                val currentMonth = DateUtil.getMonthName(event.startDate, DateUtil.DATE_FORMAT_yyyy_MM_dd)

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
                eventFullWeekDate.text = weekRange.first

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
                    val startTime = DateUtil.longToString(event.startTime,
                        DateUtil.TIME_FORMAT_HH_mm
                    )
                    "$startTime (${
                        DateUtil.getDuration(startTimestamp = event.startTime, endTimestamp = DateUtil.mergeDateAndTime(
                            DateUtil.stringToLong(event.endDate), event.endTime))})" //- $endTime"
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


