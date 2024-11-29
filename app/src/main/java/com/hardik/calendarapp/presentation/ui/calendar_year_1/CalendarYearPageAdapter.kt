package com.hardik.calendarapp.presentation.ui.calendar_year_1

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.databinding.ItemYearPage1Binding
import com.hardik.calendarapp.presentation.ui.custom_view.CustomViewYear

class CalendarYearPageAdapter() :
    RecyclerView.Adapter<CalendarYearPageAdapter.MonthViewHolder>() {

    private var yr:Int = 0
    @SuppressLint("NotifyDataSetChanged")
    fun updateYear(year:Int) {
        yr = year
        notifyItemChanged(START_POSITION) // Update only the central item
        Log.i("TAG", "updateYear: $yr")
    }
    companion object {
        const val FAKE_TOTAL_COUNT = 10000// Int.MAX_VALUE // Simulate infinite pages
        const val START_POSITION = FAKE_TOTAL_COUNT / 2 // Central position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemYearPage1Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        // Calculate the adjusted year based on the position
        //val adjustedYear = yr + (position - START_POSITION)
        Log.i("TAG", "onBindViewHolder: $yr")
        holder.bind()
    }

    override fun getItemCount(): Int = FAKE_TOTAL_COUNT

    inner class MonthViewHolder(val binding: ItemYearPage1Binding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.apply {
                Log.i("TAG", "bind:")
                customViewYear.apply {
                    //this.updateYear(year = a)
                    //currentYear = 2070
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



