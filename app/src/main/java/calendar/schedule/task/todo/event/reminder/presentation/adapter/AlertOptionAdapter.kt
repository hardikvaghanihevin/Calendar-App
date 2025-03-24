package calendar.schedule.task.todo.event.reminder.presentation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import calendar.schedule.task.todo.event.reminder.R
import calendar.schedule.task.todo.event.reminder.data.database.entity.AlertOffset

data class AlertOptionItem(
    val name: String,
    var isSelected: Boolean = false // This flag tracks whether the item is selected or not
)
class AlertOptionAdapter(
    private val context: Context,
    private val items: List<AlertOptionItem>,
    private val onItemSelected: (Int) -> Unit, // Callback for normal item clicks
    private val onCustomTimeSelected: () -> Unit // Callback to open custom time dialog
) : RecyclerView.Adapter<AlertOptionAdapter.AlertOptionViewHolder>() {

    private var lastSelectedPosition = -1 // Track the last selected position
    private var customTime: Int? = AlertOffset.BEFORE_CUSTOM_TIME.value?.toInt() // Store the custom time

    fun updateCustomTime(time: Int) {
        customTime = time
        val customTimePosition = items.indexOfFirst { it.name == context.getString(R.string.before_custom_time) }
        if (customTimePosition != -1) {
            // Set "Custom Time" item as selected
            items[customTimePosition].isSelected = true
            notifyItemChanged(customTimePosition)
        }

        // Deselect the previously selected item, if necessary
        if (lastSelectedPosition != customTimePosition && lastSelectedPosition != -1) {
            items[lastSelectedPosition].isSelected = false
            notifyItemChanged(lastSelectedPosition)
        }

        lastSelectedPosition = customTimePosition
    }

    init {
        // Find the preselected position if any
        items.forEachIndexed { index, item ->
            if (item.isSelected) {
                lastSelectedPosition = index
            }
        }
    }

    inner class AlertOptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.itemSelection_text)
        val customTimeText: TextView = view.findViewById(R.id.itemSelection_text_customTime)
        val selectIcon: ImageView = view.findViewById(R.id.itemSelection_text_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertOptionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_repeat_alert_selection, parent, false)
        return AlertOptionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AlertOptionViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = items[position]
        holder.titleText.text = item.name
        holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        holder.titleText.typeface = ResourcesCompat.getFont(context, R.font.post_nord_sans_regular)

        // Set the icon based on whether the item is selected
        if (item.isSelected) {
            holder.selectIcon.setImageResource(R.drawable.icon_checked)  // Selected icon
            if (item.name == ContextCompat.getString(context, R.string.before_custom_time)){
                holder.customTimeText.apply {
                    text = "$customTime " + ContextCompat.getString(context, R.string.min)
                    visibility = View.VISIBLE
                }
            }else{
                holder.customTimeText.visibility = View.GONE
            }

        } else {
            holder.selectIcon.setImageResource(R.drawable.icon_unchecked)  // Unselected icon
            holder.customTimeText.visibility = View.GONE
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            if (item.name == context.getString(R.string.before_custom_time)) {
                onCustomTimeSelected() // Notify fragment to open dialog
            } else {
                handleItemClick(position, item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun handleItemClick(position: Int, item: AlertOptionItem) {
        // Deselect previously selected item
        if (lastSelectedPosition != -1) {
            items[lastSelectedPosition].isSelected = false
            notifyItemChanged(lastSelectedPosition)
        }

        // Select current item
        item.isSelected = true
        notifyItemChanged(position)
        lastSelectedPosition = position

        // Notify fragment about the selected item
        onItemSelected(position)
    }

}

