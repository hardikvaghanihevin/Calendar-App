package com.hardik.calendarapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
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
    fun submitFullList(list: List<CountryItem>, query: String? = "") {
        originalList = list
        filteredList = if (query.isNullOrEmpty()) { list } else {
            list.filter { it.name.contains(query, ignoreCase = true) }
                //.sortedBy { it.name }  // Sort the filtered list by name
        }
        submitList(filteredList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val binding = when(viewType){
            VERTICAL -> { ItemCountrySelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false) }
            HORIZONTAL -> { ItemCountrySelectionSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)}
            else -> { ItemCountrySelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false) }
        }

        return CountryViewHolder(binding)
    }
   override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
       val item = filteredList[position] // Use the filtered list

       holder.countryName.text = item.name
//        holder.countryName.setTextColor(ContextCompat.getColor(holder.countryName.context, R.color.text_primary))
       holder.countryName.typeface = ResourcesCompat.getFont(holder.countryName.context, R.font.post_nord_sans_regular)

       // Set flag and checkbox based on viewType and selection state
       updateCheckboxState(holder, item)

       // Handle checkbox click for horizontal viewType
       if (viewType == HORIZONTAL) {
           holder.countryCheckbox.setOnClickListener {
               val isNowSelected = item.isSelected
               updateCheckboxState(holder, item)
               onCountryChecked(item.code, isNowSelected)
           }
       }else{
           // Handle item click
           holder.itemView.setOnClickListener {
               val isNowSelected = item.isSelected
               updateCheckboxState(holder, item)
               onCountryChecked(item.code, isNowSelected)
           }
       }
   }

    // Helper function to update checkbox state
    private fun updateCheckboxState(holder: CountryViewHolder, item: CountryItem) {
        val checkboxResource = when {
            item.isSelected -> R.drawable.icon_checked.takeIf { viewType == VERTICAL } ?: R.drawable.icon_cancel
            viewType == VERTICAL -> R.drawable.icon_unchecked
            else -> R.drawable.icon_cancel
        }

        holder.countryCheckbox.setImageResource(checkboxResource)
        if (viewType == VERTICAL) {
            holder.countryFlag?.setImageResource(item.flag)
        }
    }

    override fun getItemViewType(position: Int): Int { return viewType }

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
