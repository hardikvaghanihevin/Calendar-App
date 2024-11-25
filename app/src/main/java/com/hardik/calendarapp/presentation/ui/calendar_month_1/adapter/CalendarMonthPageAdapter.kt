package com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.databinding.ItemMonthPage1Binding
import com.hardik.calendarapp.presentation.ui.custom_view.CustomView

class CalendarMonthPageAdapter() :
    RecyclerView.Adapter<CalendarMonthPageAdapter.MonthViewHolder>() {

    companion object {
        const val FAKE_TOTAL_COUNT = Int.MAX_VALUE // Simulate infinite pages
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        //val monthIndex = position % 12
        holder.bind()
    }

    override fun getItemCount(): Int = FAKE_TOTAL_COUNT

    inner class MonthViewHolder(val binding: ItemMonthPage1Binding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.apply {
                    customView.apply {
                    postInvalidate() // Redraw the custom view if needed
                    getObjectOfCustomView?.invoke(this)
                }
            }
        }
    }

    private var getObjectOfCustomView: ((CustomView) -> Unit)? = null
    fun setObjectOfCustomView(block: (CustomView) -> Unit) {
        getObjectOfCustomView = block
    }

}



