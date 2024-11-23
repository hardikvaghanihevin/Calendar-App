package com.hardik.calendarapp.presentation.ui.calendar_month.adapter

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ItemMonthPageBinding
import com.hardik.calendarapp.domain.model.CalendarModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener

class CalendarMonthPagerAdapter(private val monthData: List<List<CalendarModel>>, private val dateItemClickListener: DateItemClickListener) :
    RecyclerView.Adapter<CalendarMonthPagerAdapter.MonthViewHolder>() {
    private val TAG = BASE_TAG + CalendarMonthPagerAdapter::class.java.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthPageBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val days = monthData[position]
        holder.bind(days, dateItemClickListener)
    }

    override fun getItemCount(): Int = monthData.size

    inner class MonthViewHolder(private val binding: ItemMonthPageBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("NotifyDataSetChanged")
        fun bind(days: List<CalendarModel>, dateItemClickListener:DateItemClickListener) {

            binding.apply {
                Log.d(TAG, "bind: called")

                // Set up layout manager and adapter if they haven't been set up yet
                if (rvCalenderMonth.layoutManager == null) {
                    rvCalenderMonth.layoutManager = GridLayoutManager(binding.root.context, 7)
                    rvCalenderMonth.addItemDecoration(object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(
                            outRect: Rect,
                            view: View,
                            parent: RecyclerView,
                            state: RecyclerView.State
                        ) {
                            outRect.top = 0
                            outRect.bottom = 0
                        }
                    })
                    rvCalenderMonth.setHasFixedSize(true)
                }
                // Update adapter data without re-creating the adapter
                (rvCalenderMonth.adapter as? CalendarMonthAdapter)?.updateData(ArrayList(days)) ?: run { rvCalenderMonth.adapter = CalendarMonthAdapter(ArrayList(days), dateItemClickListener) }
                (rvCalenderMonth.adapter as? CalendarMonthAdapter)?.setNotifyDataSetChanged()

            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNotifyDataSetChanged(){
        monthData
        notifyDataSetChanged()
    }
}






