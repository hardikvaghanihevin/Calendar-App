package com.hardik.calendarapp.presentation.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
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
import com.hardik.calendarapp.utillities.DateUtil.isAllDay
import com.hardik.calendarapp.utillities.ImageColorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
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
        /*val eventJson = GsonUtil.toJson(list)
        LogUtil.logLongMessage(TAG, "onViewCreated: $eventJson")*/

        Log.e(TAG, "updateData: ${list.size}")

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
                    binding.tvItemEventMonthName.text = currentMonth

                    val imageUrl = ImageColorUtil.monthImgResource.get(event.month.toInt())
                    //val imageUrl = ContextCompat.getDrawable(binding.root.context,R.drawable.bkg_01_jan)
                    //val rawResourceUri = Uri.parse("android.resource://${binding.root.context.packageName}/${R.raw.data_test}")

                    Glide.with(imgItemEventLayMonthTransitionImage.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bkg_01_jan)
                        .error(R.drawable.bkg_01_jan)
                        .into(imgItemEventLayMonthTransitionImage)

                   /* // Cancel any previously running job for this ViewHolder
                    scope.coroutineContext.cancelChildren()

                    val paletteCache = mutableMapOf<Int, Int>() // Store colors by resource ID
                    // Set the text color dynamically based on the image
                    scope.launch(Dispatchers.Default) {
                        val cachedColor = paletteCache[imageUrl]
                        val baseColor = if (cachedColor != null) {
                            cachedColor
                        } else {
                            val drawable = binding.root.context.getDrawable(imageUrl) as BitmapDrawable
                            val bitmap = drawable.bitmap
                            val palette = Palette.from(bitmap).generate()
                            palette.vibrantSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: 0xFF000000.toInt().also {
                                paletteCache[imageUrl] = it // Cache the color
                            }
                        }

                        // Darken the extracted color
                        val textColor = darkenColor(baseColor, 0.8f)

                        // Update UI on the main thread
                        withContext(Dispatchers.Main) {
                            tvItemEventMonthName.setTextColor(textColor)
                        }
                    }*/
                } else {
                    cardItemEventImg.visibility = View.GONE // Show the CardView
                    imgItemEventLayMonthTransitionImage.visibility = View.GONE // Hide the image
                }


                val lottieAnimationView = binding.lottieAnimationView // Ensure you have a LottieAnimationView in your layout
                lottieAnimationView.setAnimation(R.raw.data_test) // Set the raw JSON file
                lottieAnimationView.loop(true)
                lottieAnimationView.playAnimation() // Start the animation

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
                        eventFullWeekDate.visibility = View.GONE
                        if (imgItemEventLayMonthTransitionImage.isVisible && cardItemEventImg.isVisible){ }
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
        //for image scroll in sideImage view
        fun updateParallaxOffset(offset: Int) {
            // Apply translationY to create the parallax effect
            binding.imgItemEventLayMonthTransitionImage.translationY = offset.toFloat()
        }
        //for image scroll in sideImage view
        fun updateParallaxOffset(imageHeight: Int, viewTop: Int, viewBottom: Int, recyclerViewHeight: Int) {
            // Calculate how much of the RecyclerView is visible in relation to this item
            val visibleHeight = viewBottom.coerceAtMost(recyclerViewHeight) - viewTop.coerceAtLeast(0)

            // Calculate offset based on the visible part of the RecyclerView
            val totalScrollableDistance = imageHeight - visibleHeight
            val scrollOffset = ((viewTop.toFloat() / recyclerViewHeight) * totalScrollableDistance).toInt()

            // Set translationY for parallax effect
            binding.imgItemEventLayMonthTransitionImage.translationY = -scrollOffset.toFloat()
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
