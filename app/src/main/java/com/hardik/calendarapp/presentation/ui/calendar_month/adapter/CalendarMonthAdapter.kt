package com.hardik.calendarapp.presentation.ui.calendar_month.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ItemDateForMonthCalendarLayoutBinding
import com.hardik.calendarapp.domain.model.CalendarDayModel
import com.hardik.calendarapp.domain.repository.DateItemClickListener

class CalendarMonthAdapter(private var list: ArrayList<CalendarDayModel>, private val dateItemClickListener: DateItemClickListener):RecyclerView.Adapter<CalendarMonthAdapter.ViewHolder>() {
    private final val TAG = BASE_TAG + CalendarMonthAdapter::class.java.simpleName

    init { for (i in list){
//        Log.e(TAG, "init $i" )
    } }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: ArrayList<CalendarDayModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    //region Optional: If you only need to change a small set of data, you can use more specific methods
    //endregion
    @SuppressLint("NotifyDataSetChanged")
    fun updateDataSet(newList: ArrayList<CalendarDayModel>) {
        list = newList
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, newItem: CalendarDayModel) {
        list[position] = newItem
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDateForMonthCalendarLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
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

    inner class ViewHolder(private val binding: ItemDateForMonthCalendarLayoutBinding): RecyclerView.ViewHolder(binding.root) {

        private val layout = binding.root

        @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
        fun bind(calendarDayModel: CalendarDayModel, position: Int){
            val day = calendarDayModel.day

            binding.apply {
                num.text = "$day"

                binding.eventIndicator.visibility = if(calendarDayModel.eventIndicator) View.VISIBLE else View.INVISIBLE

                if (calendarDayModel.isSelected)
                    binding.constraintLay1.background = ContextCompat.getDrawable(itemView.context, R.drawable.month_navigator_shape_1)
                else
                    binding.constraintLay1.background = null

                when(calendarDayModel.state) {
                    -1->{
                        itemCalendarDateLayout.visibility = View.INVISIBLE
                    }
                    0->{
                        num.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                        today.visibility = View.INVISIBLE
                    }
                    1->{
                        // past days
                        num.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                        num.background = if (calendarDayModel.isHoliday) ContextCompat.getDrawable(itemView.context,R.drawable.shape_circle_red) else ContextCompat.getDrawable(itemView.context, R.drawable.shape_circle_green)
                        today.visibility = View.INVISIBLE
                        layout.setOnClickListener {

                            selectItem(position = position)
                            dateItemClickListener.onDateClick(position = position, calendarDayModel) }
                    }
                    2->{
                        // today
                        num.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                        num.background = ContextCompat.getDrawable(itemView.context, R.drawable.shape_circle_blue)
                        today.visibility = View.VISIBLE
                        layout.setOnClickListener {

                            selectItem(position = position)
                            dateItemClickListener.onDateClick(position = position, calendarDayModel) }
                    }
                    3 -> {
                        // future days
                        num.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                        num.background = if (calendarDayModel.isHoliday) ContextCompat.getDrawable(itemView.context,R.drawable.shape_circle_red) else ContextCompat.getDrawable(itemView.context, R.drawable.shape_circle_gray)
                        today.visibility = View.INVISIBLE
                        layout.setOnClickListener {

                            selectItem(position = position)
                            dateItemClickListener.onDateClick(position = position, calendarDayModel) }
                    }
                }
            }
        }
        // This function is responsible for selecting the clicked item and deselecting others
        @SuppressLint("NotifyDataSetChanged")
        private fun selectItem(position: Int) {
            Log.e(TAG, "selectItem: ", )
            // Deselect all items
            list.forEach { it.isSelected = false }
            // Select the clicked item
            list[position].isSelected = true
            notifyDataSetChanged() // Notify the adapter to update the UI
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNotifyDataSetChanged(){
        list
        notifyDataSetChanged()
    }
}