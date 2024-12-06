package com.hardik.calendarapp.presentation.ui.view_event

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.databinding.FragmentViewEventBinding
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewEventFragment : Fragment(R.layout.fragment_view_event) {
    private val TAG = BASE_TAG + ViewEventFragment::class.java.simpleName

    private var _binding: FragmentViewEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var argEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            argEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(Constants.KEY_EVENT, Event::class.java) ?: throw IllegalArgumentException("Event is missing")
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(Constants.KEY_EVENT) ?: throw IllegalArgumentException("Event is missing")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentViewEventBinding.bind(view)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate the menu resource for the fragment
                menuInflater.inflate(R.menu.view_event_menu, menu)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        navigateToNewEventFragForEdit(argEvent)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add it for this fragment's lifecycle

        if (arguments?.containsKey(KEY_EVENT) == true){
            Log.e(TAG, "onViewCreated: argEvent:$argEvent", )
            populateEventData(event = argEvent)

        }
    }

    private fun populateEventData(event: Event) {
        // Populate the title and description
        binding.tInEdtEventName.setText(event.title)
        binding.tInEdtEventNote.setText(event.description.takeUnless { it.isBlank() } ?: resources.getString(R.string.no_description))
        binding.switchAllDay.isChecked = DateUtil.isAllDay(startTime = event.startTime, endTime = event.endTime)

        // Set the "All Day" status
        binding.switchAllDay.isChecked = DateUtil.isAllDay(startTime = event.startTime, endTime = event.endTime)

        // Populate start and end dates
        binding.tvStartDatePicker.text = DateUtil.stringToString(
            dateString = event.startDate,
            inputPattern = DateUtil.DATE_FORMAT_yyyy_MM_dd,
            outputPattern = DateUtil.DATE_FORMAT_dd_MMM_yyyy
        )
        binding.tvEndDatePicker.text = DateUtil.stringToString(
            dateString = event.endDate,
            inputPattern = DateUtil.DATE_FORMAT_yyyy_MM_dd,
            outputPattern = DateUtil.DATE_FORMAT_dd_MMM_yyyy
        )

        // Populate startTime and endTime
        if (binding.switchAllDay.isChecked){
            binding.tvStartTimePicker.visibility = View.GONE
            binding.tvEndTimePicker.visibility = View.GONE
        }else{
            binding.tvStartTimePicker.visibility = View.VISIBLE
            binding.tvEndTimePicker.visibility = View.VISIBLE
        }
        binding.tvStartTimePicker.text = DateUtil.longToString(
            timestamp = event.startTime,
            pattern = DateUtil.TIME_FORMAT_hh_mm_a
        )
        binding.tvEndTimePicker.text = DateUtil.longToString(
            timestamp = event.endTime,
            pattern = DateUtil.TIME_FORMAT_hh_mm_a
        )


        /*// Update ViewModel with the data
        viewModel.updateTitle(event.title)
        viewModel.updateDescription(event.description)
        viewModel.updateStartDate(DateUtil.stringToLong(event.startDate))
        viewModel.updateEndDate(DateUtil.stringToLong(event.endDate))
        viewModel.updateStartTime(event.startTime)
        viewModel.updateEndTime(event.endTime)
        viewModel.updateAllDayStatus(false)//event.isAllDay)*/
    }

    private fun navigateToNewEventFragForEdit(event: Event) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            Log.e(TAG, "navigateToNewEventFragForEdit:  ${Thread.currentThread().name}", )
            val bundle = Bundle().apply {
                putParcelable(Constants.KEY_EVENT, event)// Pass the event object
            }
            findNavController().navigate(R.id.newEventFragment, bundle)
        }
    }


}