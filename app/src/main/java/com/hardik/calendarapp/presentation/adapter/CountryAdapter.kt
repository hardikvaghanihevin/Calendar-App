package com.hardik.calendarapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ItemCountrySelectionBinding
import com.hardik.calendarapp.databinding.ItemCountrySelectionSmallBinding

data class CountryItem(
    val flag: Int,
    val name: String,
    val code: String,
    val isSelected: Boolean
)
const val VERTICAL = 0
const val HORIZONTAL = 1
class CountryAdapter(private val onCountryChecked: (String, Boolean) -> Unit, private val viewType: Int = VERTICAL) : ListAdapter<CountryItem, CountryAdapter.CountryViewHolder>(CountryDiffCallback()) , Filterable {
    private val TAG = BASE_TAG + CountryAdapter::class.java.simpleName

    private var originalList = listOf<CountryItem>() // Store the full list
    private var filteredList = listOf<CountryItem>() // Store the filtered list

    // Update the adapter list
    fun submitFullList(list: List<CountryItem>) {
        originalList = list
        filteredList = list
        submitList(filteredList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        //val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country_language_selection, parent, false)
        val binding = when(viewType){
            VERTICAL -> { ItemCountrySelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false) }
            HORIZONTAL -> { ItemCountrySelectionSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)}
            else -> { ItemCountrySelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false) }
            //else -> throw IllegalArgumentException("Unknown binding type")
        }

        return CountryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        //val item = getItem(position)// Use Default item of list if needed
        val item = filteredList[position] // Use the filtered list

        holder.countryName.text = item.name
        holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.text_primary))
        holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_medium)

        if(viewType == VERTICAL){
            holder.countryFlag?.setImageResource(item.flag)
            if (item.isSelected) {
                holder.countryCheckbox.setImageResource(R.drawable.icon_checked)
                //holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.accent_primary))
                //holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_medium)
            } else {
                holder.countryCheckbox.setImageResource(R.drawable.icon_unchecked)
                //holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.text_primary))
                //holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_regular)
            }
        }else{
            if (item.isSelected) {
                holder.countryCheckbox.setImageResource(R.drawable.icon_cancel)
            } else {
                holder.countryCheckbox.setImageResource(R.drawable.icon_cancel)
            }
        }

        holder.itemView.setOnClickListener {
            //val isNowSelected = !selectedCountries.contains(item.code)// todo: used adapter data list base selected or unselected
            val isNowSelected = item.isSelected//todo: to importance it's set from viewModel base selected or unselected
            if(viewType == VERTICAL){
                if (isNowSelected) {
                    holder.countryCheckbox.setImageResource(R.drawable.icon_checked)
                    //holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.accent_primary))
                    //holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_medium)
                } else {
                    holder.countryCheckbox.setImageResource(R.drawable.icon_unchecked)
                    //holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.text_primary))
                    //holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_regular)
                }
            }else{
                if (isNowSelected) {
                    holder.countryCheckbox.setImageResource(R.drawable.icon_cancel)
                } else {
                    holder.countryCheckbox.setImageResource(R.drawable.icon_cancel)
                }
            }

            // Callback to the parent activity/fragment with the selected state
            onCountryChecked(item.code, isNowSelected)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return viewType
        /*when (position) {
            0 -> VERTICAL   // For the first item, use VERTICAL view type
            1 -> HORIZONTAL // For the second item, use HORIZONTAL view type
            else -> 0       // Default case if no match is found
        }*/
    }

    inner class CountryViewHolder(private val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val countryName: TextView
        val countryCheckbox: ImageView
        val countryFlag: ImageView? // Optional, only used in VERTICAL

        init {
            when (binding) {
                is ItemCountrySelectionBinding -> {
                    countryName = binding.itemSelectionText
                    countryCheckbox = binding.itemSelectionTextIcon
                    countryFlag = binding.itemSelectionFlagSImg // Ensure this matches your layout file
                }
                is ItemCountrySelectionSmallBinding -> {
                    countryName = binding.itemSelectionText
                    countryCheckbox = binding.itemSelectionTextIcon
                    countryFlag = null // Not used in HORIZONTAL
                }
                else -> throw IllegalArgumentException("Unknown binding type")
            }
        }
    }

    private class CountryDiffCallback : DiffUtil.ItemCallback<CountryItem>() {
        override fun areItemsTheSame(oldItem: CountryItem, newItem: CountryItem): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: CountryItem, newItem: CountryItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""
                val results = if (query.isEmpty()) {
                    originalList
                } else {
                    originalList.filter { it.name.lowercase().contains(query) }
                }

                return FilterResults().apply { values = results }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<CountryItem> ?: originalList
                submitList(filteredList)
            }
        }
    }
}
