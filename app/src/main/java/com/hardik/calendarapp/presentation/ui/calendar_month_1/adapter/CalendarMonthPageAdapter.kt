package com.hardik.calendarapp.presentation.ui.calendar_month_1.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.databinding.ItemMonthPage1Binding

class CalendarMonthPageAdapter(private val months: List<String>) : RecyclerView.Adapter<CalendarMonthPageAdapter.MonthViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_month_page1, parent, false)
        val binding = ItemMonthPage1Binding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val days = months[position]
        holder.bind(days, position)
    }

    override fun getItemCount(): Int = months.size

    inner class MonthViewHolder(private val binding: ItemMonthPage1Binding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(month: String, position: Int) {
            binding.apply {
                customView.apply {
                    currentMonth = position
                }
            }
        }
    }
}
