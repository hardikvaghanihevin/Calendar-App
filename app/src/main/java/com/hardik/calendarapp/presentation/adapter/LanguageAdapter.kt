package com.hardik.calendarapp.presentation.adapter

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
import com.hardik.calendarapp.R

data class LanguageItem(
    val name: String,
    var isSelected: Boolean = false // This flag tracks whether the item is selected or not
)
class LanguageAdapter(
    private val context: Context,
    private val items: List<LanguageItem>,
    private val onItemSelected: (Int) -> Unit // Callback to notify when an item is selected
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var lastSelectedPosition = -1 // Track the last selected position

    init {
        // Find the preselected position if any
        items.forEachIndexed { index, item ->
            if (item.isSelected) {
                lastSelectedPosition = index
            }
        }
    }

    inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val languageText: TextView = view.findViewById(R.id.itemSelection_text)
        val icon: ImageView = view.findViewById(R.id.itemSelection_text_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_country_language_selection, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = items[position]
        holder.languageText.text = item.name

        // Set the icon based on whether the item is selected
        if (item.isSelected) {
            holder.icon.setImageResource(R.drawable.checked_icon)  // Selected icon
            holder.languageText.setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
            holder.languageText.typeface = ResourcesCompat.getFont(context, R.font.post_nord_sans_medium)
        } else {
            holder.icon.setImageResource(R.drawable.unchecked_icon)  // Unselected icon
            holder.languageText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.languageText.typeface = ResourcesCompat.getFont(context, R.font.post_nord_sans_regular)
        }

        holder.itemView.setOnClickListener {
            // Deselect the previously selected item
            if (lastSelectedPosition != -1) {
                items[lastSelectedPosition].isSelected = false
                notifyItemChanged(lastSelectedPosition) // Only update the previously selected item
            }

            // Select the clicked item
            item.isSelected = true
            notifyItemChanged(position)  // Update only the clicked item

            // Update the position of the selected item
            lastSelectedPosition = position

            // Notify the activity/fragment
            onItemSelected(position)
        }
    }

    override fun getItemCount(): Int = items.size
}

