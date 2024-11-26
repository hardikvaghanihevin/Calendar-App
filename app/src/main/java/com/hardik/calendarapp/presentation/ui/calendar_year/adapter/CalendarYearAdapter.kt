package com.hardik.calendarapp.presentation.ui.calendar_year.adapter

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ItemMonthForYearCalendarLayoutBinding
import com.hardik.calendarapp.domain.model.CalendarDayModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener
import com.hardik.calendarapp.utillities.getMonth

class CalendarYearAdapter(private val list: ArrayList<List<CalendarDayModel>> = arrayListOf(), private val dateItemClickListener: DateItemClickListener) : RecyclerView.Adapter<CalendarYearAdapter.ViewHolder>() {
    private val TAG = BASE_TAG + CalendarYearAdapter::class.java.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMonthForYearCalendarLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
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
        holder.bind(list[position], position = position,dateItemClickListener = dateItemClickListener)
    }

    inner class ViewHolder(private val binding: ItemMonthForYearCalendarLayoutBinding): RecyclerView.ViewHolder(binding.root) {
        private val layout = binding.root

        @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
        fun bind(months: List<CalendarDayModel>, position: Int, dateItemClickListener: DateItemClickListener){

            binding.apply {

                tvMonthOfYear.text = "${getMonth(position+1,binding.root.context)}"

                rvMonthForYearCalender.layoutManager = GridLayoutManager(binding.root.context, 7)
                rvMonthForYearCalender.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
                rvMonthForYearCalender.setHasFixedSize(true)
                // Update adapter data without re-creating the adapter
                (rvMonthForYearCalender.adapter as? CalendarMonthOfYearAdapter)?.updateData(ArrayList(months)) ?: run { rvMonthForYearCalender.adapter = CalendarMonthOfYearAdapter(ArrayList(months), dateItemClickListener) }
                (rvMonthForYearCalender.adapter as? CalendarMonthOfYearAdapter)?.setNotifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: ArrayList<List<CalendarDayModel>>) {
        Log.d(TAG, "updateData: newlist: ${newList}")
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setNotifyDataSetChanged(){
        list
        notifyDataSetChanged()
    }
}