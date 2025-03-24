package calendar.schedule.task.todo.event.reminder.presentation.adapter.diff_util

import androidx.recyclerview.widget.DiffUtil
import calendar.schedule.task.todo.event.reminder.data.database.entity.Event

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        //return oldItem == newItem
        return oldItem.id == newItem.id &&
                oldItem.title == newItem.title &&
                oldItem.startTime == newItem.startTime &&
                oldItem.endTime == newItem.endTime &&
                oldItem.startDate == newItem.startDate &&
                oldItem.endDate == newItem.endDate &&
                oldItem.year == newItem.year &&
                oldItem.month == newItem.month &&
                oldItem.date == newItem.date &&
                oldItem.isHoliday == newItem.isHoliday &&
                oldItem.eventType == newItem.eventType &&
                oldItem.sourceType == newItem.sourceType &&
                oldItem.description == newItem.description &&
                oldItem.repeatOption == newItem.repeatOption &&
                oldItem.alertOffset == newItem.alertOffset &&
                oldItem.customAlertOffset == newItem.customAlertOffset &&
                oldItem.triggerTime == newItem.triggerTime

    }
}