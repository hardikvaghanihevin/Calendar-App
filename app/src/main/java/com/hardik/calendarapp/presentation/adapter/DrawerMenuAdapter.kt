package com.hardik.calendarapp.presentation.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.R

data class DrawerMenuItem(
    val iconAttr: Int,
    val title: String,
    val id: Int,
    var isSelected: Boolean = false // Track selected state
)
class DrawerMenuAdapter(
    private val items: List<DrawerMenuItem>,
    private val onClick: (DrawerMenuItem) -> Unit
) : RecyclerView.Adapter<DrawerMenuAdapter.ViewHolder>() {

    private var selectedPosition: Int = 0 // Track the currently selected position (-1 not set, 0 for index 1)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.drawer_menu_item_container)
        val icon: ImageView = view.findViewById(R.id.item_icon)
        val title: TextView = view.findViewById(R.id.item_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.drawer_menu_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Update view for the selected state
        // Set the icon and title for the drawer item
        holder.icon.setImageDrawable(getDrawableFromAttribute(holder.itemView.context, item.iconAttr))
        holder.title.text = item.title

        // Check if the item is selected and update UI accordingly
        if (position == selectedPosition) {
            holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.accent_primary))
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.accent_primary))
            holder.container.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.background_primary))
        } else {
            holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
            holder.container.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.background_primary))
        }

        // Handle item click
        holder.container.setOnClickListener {
            // Store the current selected position before updating
            val previousSelectedPosition = selectedPosition

            onClick(item) ;
            //setSelectedPosition(position)

            // Check if the item title is "Jump to Date", "App Theme", or "First Day of the Week"
            if (item.title == "Jump to Date" || item.title == "App Theme" || item.title == "First Day of the Week" || item.title == "Device Information") {
                // If title matches, unselect the item (clear selection)
                selectedPosition = previousSelectedPosition//-1 // or any other logic for unselecting

            } else {
                // Otherwise, set the selected position
                setSelectedPosition(position)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // Method to update the selected position and notify the adapter to update the UI
    fun updateSelectedItem(selectedId: Int) {
        // Find the item with the selectedId and update the selected position
        val newSelectedPosition = items.indexOfFirst { it.id == selectedId }
        if (newSelectedPosition != -1 && newSelectedPosition != selectedPosition) {
            setSelectedPosition(newSelectedPosition)
        }
    }

    // Update the selected position and notify the adapter to update the UI
    private fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position

        // Update UI for both previous and new selected positions
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }
}
fun getDrawableFromAttribute(context: Context, attr: Int): Drawable? {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attr, typedValue, true)
    return ContextCompat.getDrawable(context, typedValue.resourceId)
}
