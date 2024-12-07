package com.hardik.calendarapp.presentation.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.ItemEventLayoutBinding
import com.hardik.calendarapp.domain.repository.DateItemClickListener
import com.hardik.calendarapp.utillities.DateUtil
import java.util.Date

//class EventAdapter(private var list: ArrayList<CalendarDetail.Item>, private val dateItemClickListener: DateItemClickListener):
class EventAdapter(private var list: ArrayList<Event>, private val dateItemClickListener: DateItemClickListener):
    RecyclerView.Adapter<EventAdapter.ViewHolder>() {
    private final val TAG = BASE_TAG + EventAdapter::class.java.simpleName

    init {
        for (i in list) { //Log.e(TAG, "init $i" )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Event>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
        Log.e(TAG, "updateData: ${list.size}")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: ArrayList<Event>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    //region Optional: If you only need to change a small set of data, you can use more specific methods
    //endregion
    @SuppressLint("NotifyDataSetChanged")
    fun updateDataSet(newList: ArrayList<Event>) {
        list = newList
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, newItem: Event) {
        list[position] = newItem
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemEventLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        binding.root.apply {
            layoutParams = lp
        }
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position], position = position)
    }

    inner class ViewHolder(private val binding: ItemEventLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val layout = binding.root

        @SuppressLint("SetTextI18n")
        fun bind(event: Event, position: Int) {
            binding.apply {
                binding.itemEventLayout.visibility = if (event.title == "Dummy") View.INVISIBLE else View.VISIBLE
                eventDate.text = event.startDate
                eventTitle.text = event.title
                eventTimePeriod.text = DateUtil.longToString(event.endTime, DateUtil.DATE_FORMAT_yyyy_MM_dd)
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

}
