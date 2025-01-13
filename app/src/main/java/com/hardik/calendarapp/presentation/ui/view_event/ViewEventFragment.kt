package com.hardik.calendarapp.presentation.ui.view_event

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.EventType
import com.hardik.calendarapp.data.database.entity.RepeatOptionConverter
import com.hardik.calendarapp.databinding.FragmentViewEventBinding
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.new_event.NewEventViewModel
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.TIME_FORMAT_HH_mm
import com.hardik.calendarapp.utillities.MyNavigation.navOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewEventFragment : Fragment(R.layout.fragment_view_event) {
    private val TAG = BASE_TAG + ViewEventFragment::class.java.simpleName

    private val viewModel: NewEventViewModel by activityViewModels()
    private var _binding: FragmentViewEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var argEvent: Event

    var is24HourFormat = false

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
        Log.i(TAG, "onViewCreated: ")
        _binding = FragmentViewEventBinding.bind(view)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        is24HourFormat = sharedPreferences.getBoolean("time_format", false)

        if (arguments?.containsKey(KEY_EVENT) == true){
            Log.e(TAG, "onViewCreated: argEvent:$argEvent", )
            populateEventData(event = argEvent)
        }

        /** Delete Event  */
        (activity as MainActivity).binding.appBarMain.deleteEventIcon.apply {

            if (arguments?.containsKey(KEY_EVENT) == true){
                if (argEvent.eventType != EventType.PERSONAL){
                    (activity as MainActivity).hideViewWithAnimation(this)
                }
            }

            text = resources.getString(R.string.action_delete)
            setOnClickListener {
                lifecycleScope.launch {
                    viewModel.deleteEvent(argEvent)
                    Snackbar.make(view, resources.getString(R.string.event_deleted), Snackbar.LENGTH_LONG).setAnchorView(binding.baseline).show()
                    viewModel.resetEventState()
                    findNavController().popBackStack(R.id.viewEventFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
        }

        /** Edit Event  */
        (activity as MainActivity).binding.appBarMain.saveEventIcon.apply {

            if (arguments?.containsKey(KEY_EVENT) == true){
                if (argEvent.eventType != EventType.PERSONAL){
                    (activity as MainActivity).hideViewWithAnimation(this)
                }
            }

            text = resources.getString(R.string.action_edit)
            setOnClickListener { navigateToNewEventFragForEdit(argEvent) }
        }
    }

    private fun populateEventData(event: Event) {
        // Populate the title and description
        binding.edtEventName.setText(event.title)
        binding.edtEventNote.setText(event.description.takeUnless { it.isBlank() } ?: resources.getString(R.string.no_description))
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
            pattern = TIME_FORMAT_HH_mm.takeIf { is24HourFormat } ?: DateUtil.TIME_FORMAT_hh_mm_a
        )
        binding.tvEndTimePicker.text = DateUtil.longToString(
            timestamp = event.endTime,
            pattern = TIME_FORMAT_HH_mm.takeIf { is24HourFormat } ?: DateUtil.TIME_FORMAT_hh_mm_a
        )

        binding.tvRepeatPicker.text = RepeatOptionConverter.toDisplayString(
            context = requireContext(),
            repeatOption = event.repeatOption
        )
        //binding.tvAlertPicker.text = AlertOffsetConverter.toDisplayString(context = requireContext(), alertOffset = event.alertOffset)
        val minutesTime = if (event.alertOffset == AlertOffset.BEFORE_CUSTOM_TIME)
            event.customAlertOffset.let {value: Long? ->
                if (value == null) "Custom time is not set"//"null"
                else {
                    //DateUtil.longToString(timestamp = it, pattern = DateUtil.DATE_TIME_FORMAT_yyyy_MM_dd_HH_mm)
                    "Before ${DateUtil.timestampToMinutes(milliseconds = value)} minute"
                }
            }
        else AlertOffsetConverter.toDisplayString(
            context = requireContext(),
            alertOffset = event.alertOffset
        )
        binding.tvAlertPicker.text = minutesTime


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
            findNavController().navigate(R.id.newEventFragment, bundle, navOptions)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: ")
        lifecycleScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView: ")

        // Check if arguments are present and contain the required keys
        if ( arguments?.containsKey(Constants.KEY_EVENT) == true ) {

        } else {
            // Fallback: No arguments,
        }

        _binding = null
    }


}