package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.databinding.ItemYearPage1Binding
import com.hardik.calendarapp.presentation.ui.custom_view.CustomViewYear

class CalendarYearPageAdapter() :
    RecyclerView.Adapter<CalendarYearPageAdapter.MonthViewHolder>() {

    companion object {
        const val FAKE_TOTAL_COUNT = Int.MAX_VALUE // Simulate infinite pages
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemYearPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = FAKE_TOTAL_COUNT

    inner class MonthViewHolder(val binding: ItemYearPage1Binding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.apply {
                customViewYear.apply {
                    postInvalidate() // Redraw the custom view if needed
                    getObjectOfCustomViewYear?.invoke(this)
                }
            }
        }
    }

    private var getObjectOfCustomViewYear: ((CustomViewYear) -> Unit)? = null
    fun setObjectOfCustomViewYear(block: (CustomViewYear) -> Unit) {
        getObjectOfCustomViewYear = block
    }
}



