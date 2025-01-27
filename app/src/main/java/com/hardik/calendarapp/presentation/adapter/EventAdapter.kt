package com.hardik.calendarapp.presentation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.ItemEventLayout1Binding
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.DATE_FORMAT_yyyy_MM_dd
import com.hardik.calendarapp.utillities.DateUtil.TIME_FORMAT_HH_mm
import com.hardik.calendarapp.utillities.DateUtil.isAllDay
import com.hardik.calendarapp.utillities.ImageColorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import java.util.Calendar
import java.util.Locale

class EventAdapter(private var list: ArrayList<Event>): RecyclerView.Adapter<EventAdapter.ViewHolder>(), Filterable {
    private val TAG = BASE_TAG + EventAdapter::class.java.simpleName

    // Keep a copy of the original list for filtering
    private var originalList: List<Event> = list.toList() // Keep an immutable copy
    private var filteredList: List<Event> = originalList

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Event>) {

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = originalList.size
            override fun getNewListSize() = newData.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // Assuming `id` is a unique identifier for `Event`
                return originalList[oldItemPosition].id == newData[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // Compare entire content or specific fields
                return originalList[oldItemPosition] == newData[newItemPosition]
            }
        }

        // Calculate the diff and update lists
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        originalList = newData // Update original list
        filteredList = originalList // Reset filtered list
        list.clear()
        list.addAll(newData)
        diffResult.dispatchUpdatesTo(this)
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
        val previousEvent = if (position > 0) filteredList[position - 1] else null
        holder.bind( filteredList[position], previousEvent, position )
    }

    inner class ViewHolder(private val binding: ItemEventLayout1Binding) :
        RecyclerView.ViewHolder(binding.root) {

        private val layout = binding.root
        private val scope = CoroutineScope(Dispatchers.Main) // Create a CoroutineScope for this ViewHolder

        @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
        fun bind(event: Event, previousEvent: Event?, position: Int) {
            binding.apply {
                // Check if the current event's month is different from the previous event
                val currentMonth = DateUtil.getMonthName(event.startDate, DATE_FORMAT_yyyy_MM_dd)
                val previousMonth = previousEvent?.let { DateUtil.getMonthName(it.startDate, DATE_FORMAT_yyyy_MM_dd) }


                // Show divider image when the month changes
                if (previousMonth != null && previousMonth != currentMonth) {
                    cardItemEventImg.visibility = View.VISIBLE // Show the CardView
                    imgItemEventLayMonthTransitionImage.visibility = View.VISIBLE // Show the image
                    binding.tvItemEventMonthName.text = "$currentMonth ${event.year}"


                    val imageUrl = ImageColorUtil.monthImgResource.get(event.month.toInt())

                    Glide.with(imgItemEventLayMonthTransitionImage.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bkg_01_jan)
                        .error(R.drawable.bkg_01_jan)
                        .into(imgItemEventLayMonthTransitionImage)

                } else {
                    cardItemEventImg.visibility = View.GONE // Show the CardView
                    imgItemEventLayMonthTransitionImage.visibility = View.GONE // Hide the image
                }

                // Check if the current event's week is the same as the previous event
                val currentEventWeek = DateUtil.getWeekOfYear(event.startDate, DATE_FORMAT_yyyy_MM_dd)
                val previousEventWeek = previousEvent?.let { DateUtil.getWeekOfYear(it.startDate, DATE_FORMAT_yyyy_MM_dd) }

                // Check if the current event's date is the same as the previous event
                if (previousEvent != null && previousEvent.startDate == event.startDate) {
                    // Hide day name and date for duplicate dates
                    eventDayDate.visibility = View.INVISIBLE
                    llItemEvent.visibility = View.GONE
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
                        llItemEvent.visibility = View.VISIBLE
                        eventFullWeekDate.visibility = View.VISIBLE
                        eventFullWeekDate.text = DateUtil.getWeekRange(dateForWeek, weekStart)

//                        eventFullWeekDate.apply {
//                            (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
//                                val currentStart = marginStart // Preserve the current start margin
//                                val currentEnd = marginEnd     // Preserve the current end margin
//                                val currentBottom = marginBottom  // Preserve the current bottom margin
//
//                                // Update only top and bottom margins
//                                setMargins(
//                                    currentStart,// Start margin
//                                    resources.getDimension(com.intuit.sdp.R.dimen._19sdp).toInt(), // Top margin
//                                    currentEnd,// End margin
//                                    currentBottom  // Bottom margin
//                                )
//                            }
//                        }

                    } else {
                        llItemEvent.visibility = View.GONE
                        eventFullWeekDate.visibility = View.GONE
                        if (imgItemEventLayMonthTransitionImage.isVisible && cardItemEventImg.isVisible){ }
                    }
                }

                // Set event title
                eventTitle.text = event.title

                // Set "All day" or time period based on start and end time
                eventTimePeriod.text = if ( isAllDay(startTime = event.startTime, endTime = event.endTime) ) ContextCompat.getString(binding.root.context, R.string.all_day)
                else {
                    val startTime = DateUtil.longToString(event.startTime, TIME_FORMAT_HH_mm)
                    val endTime = DateUtil.longToString(event.endTime, TIME_FORMAT_HH_mm)
                    if (startTime == "00:00" && endTime == "00:00") "-"
                    else "$startTime - $endTime"

                }
                // Handle item clicks
                itemEventLayout.setOnClickListener { configureEventCallBack?.invoke(event) }
            }

        }

        fun clear() {
            // Cancel the CoroutineScope to clean up resources when ViewHolder is recycled
            scope.coroutineContext.cancelChildren()
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

                noDataCallback?.invoke(true.takeUnless { filteredList.isEmpty() } ?: false) // Notify when no data is found

                notifyDataSetChanged()
            }
        }
    }

    private var noDataCallback: ((hasData: Boolean) -> Unit)? = null

    fun setNoDataCallback(callback: (Boolean) -> Unit) {
        noDataCallback = callback
    }

}
