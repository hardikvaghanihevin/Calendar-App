package com.hardik.calendarapp.presentation.ui.calendar_year.adapter

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ItemYearPageBinding
import com.hardik.calendarapp.domain.model.CalendarDayModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener

class CalendarYearPagerAdapter(
    private val yearsData: MutableList<List<List<CalendarDayModel>>>,
    private val dateItemClickListener: DateItemClickListener
) :
    RecyclerView.Adapter<CalendarYearPagerAdapter.ViewHolder>() {


    private val TAG = BASE_TAG + CalendarYearPagerAdapter::class.java.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemYearPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return yearsData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val year = yearsData[position]
        holder.bind(year, position, dateItemClickListener)
    }

    inner class ViewHolder(private val binding: ItemYearPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            years: List<List<CalendarDayModel>>,
            position: Int,
            dateItemClickListener: DateItemClickListener
        ) {
//            Log.d(TAG, "bind: ${years.size}")
            binding.apply {
                if (rvCalenderYear.layoutManager == null) {
                    rvCalenderYear.layoutManager = GridLayoutManager(binding.root.context, 3)
                    rvCalenderYear.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
                    rvCalenderYear.setHasFixedSize(true)
                }
                // Update adapter data without re-creating the adapter
                (rvCalenderYear.adapter as? CalendarYearAdapter)?.updateData(ArrayList(years))
                    ?: run {
                        rvCalenderYear.adapter =
                            CalendarYearAdapter(ArrayList(years), dateItemClickListener)
                    }
                (rvCalenderYear.adapter as? CalendarYearAdapter)?.setNotifyDataSetChanged()
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNotifyDataSetChanged() {
        yearsData
        notifyDataSetChanged()
    }
}
