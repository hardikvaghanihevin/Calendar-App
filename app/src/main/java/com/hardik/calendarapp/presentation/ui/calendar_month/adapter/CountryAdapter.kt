package com.hardik.calendarapp.presentation.ui.calendar_month.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hardik.calendarapp.R
import com.hardik.calendarapp.databinding.ItemCountryLanguageSelectionBinding

data class CountryItem(
    val name: String,
    val code: String,
    val isSelected: Boolean
)
class CountryAdapter(private val onCountryChecked: (String, Boolean) -> Unit) : ListAdapter<CountryItem, CountryAdapter.CountryViewHolder>(CountryDiffCallback()) {

    private val selectedCountries = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        //val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country_language_selection, parent, false)
        val binding = ItemCountryLanguageSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CountryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val item = getItem(position)

        holder.countryName.text = item.name

        if (item.isSelected) {
            holder.countryCheckbox.setImageResource(R.drawable.checked_icon)
            holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.accent_primary))
            holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_medium)
        } else {
            holder.countryCheckbox.setImageResource(R.drawable.unchecked_icon)
            holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.text_primary))
            holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_regular)
        }

        holder.itemView.setOnClickListener {
            val isNowSelected = !selectedCountries.contains(item.code)
            if (isNowSelected) {
                selectedCountries.add(item.code)
                holder.countryCheckbox.setImageResource(R.drawable.checked_icon)
                holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.accent_primary))
                holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_medium)
            } else {
                selectedCountries.remove(item.code)
                holder.countryCheckbox.setImageResource(R.drawable.unchecked_icon)
                holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.text_primary))
                holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_regular)
            }

            // Callback to the parent activity/fragment with the selected state
            onCountryChecked(item.code, isNowSelected)
        }
    }

    fun getSelectedCountries(): Set<String> = selectedCountries

    inner class CountryViewHolder(itemView: ItemCountryLanguageSelectionBinding) : RecyclerView.ViewHolder(itemView.root) {
        val countryName: TextView = itemView.itemSelectionText; val countryCheckbox: ImageView = itemView.itemSelectionTextIcon }

    private class CountryDiffCallback : DiffUtil.ItemCallback<CountryItem>() {
        override fun areItemsTheSame(oldItem: CountryItem, newItem: CountryItem): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: CountryItem, newItem: CountryItem): Boolean {
            return oldItem == newItem
        }
    }
}
