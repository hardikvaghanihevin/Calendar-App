package com.hardik.calendarapp.presentation.ui.new_event

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.DialogItemDatePickerBinding
import com.hardik.calendarapp.databinding.DialogItemTimePickerBinding
import com.hardik.calendarapp.databinding.FragmentNewEventBinding
import com.hardik.calendarapp.utillities.DateUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class NewEventFragment : Fragment(R.layout.fragment_new_event) {
    private val TAG = BASE_TAG + NewEventFragment::class.java.simpleName

    private val viewModel: NewEventViewModel  by activityViewModels()
    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!

    override fun onActivityCreated(savedInstanceState: Bundle?) { super.onActivityCreated(savedInstanceState) }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNewEventBinding.bind(view)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate the menu resource for the fragment
                menuInflater.inflate(R.menu.new_event_menu, menu)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save -> {
                        viewModel.insertCustomEvent()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add it for this fragment's lifecycle


        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.yearState.collectLatest { year ->
                        Log.i(TAG, "setupUI: year:$year")
                        Toast.makeText(requireContext(), "$year", Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.startDate.collectLatest { startDate ->
                        binding.tvStartDatePicker.text = DateUtil.longToString(
                            timestamp = startDate,
                            pattern = DateUtil.DATE_FORMAT_2
                        )
                    }
                }
                launch {
                    viewModel.endDate.collectLatest { endDate ->
                        binding.tvEndDatePicker.text = DateUtil.longToString(
                            timestamp = endDate,
                            pattern = DateUtil.DATE_FORMAT_2
                        )
                    }
                }
                launch {
                    viewModel.startTime.collectLatest { startTime ->
                        binding.tvStartTimePicker.text = DateUtil.longToString(
                            timestamp = startTime,
                            pattern = DateUtil.TIME_FORMAT_1
                        )
                    }
                }
                launch {
                    viewModel.endTime.collectLatest { endTime ->
                        binding.tvEndTimePicker.text = DateUtil.longToString(
                            timestamp = endTime,
                            pattern = DateUtil.TIME_FORMAT_1
                        )
                    }
                }
                launch {
                    viewModel.isAllDay.collectLatest {isAllDay ->
                        if (isAllDay){
                            binding.tvStartTimePicker.visibility = View.GONE
                            binding.tvEndTimePicker.visibility = View.GONE

                            val layoutParams = binding.tvStartDatePicker.layoutParams as ConstraintLayout.LayoutParams
                            layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, 0, layoutParams.bottomMargin) // Set marginEnd to 0dp
                            binding.tvStartDatePicker.layoutParams = layoutParams

                            val layoutParams1 = binding.tvEndDatePicker.layoutParams as ConstraintLayout.LayoutParams
                            layoutParams1.setMargins(layoutParams1.leftMargin, layoutParams1.topMargin, 0, layoutParams1.bottomMargin) // Set marginEnd to 0dp
                            binding.tvEndDatePicker.layoutParams = layoutParams1

                        }else{
                            binding.tvStartTimePicker.visibility = View.VISIBLE
                            binding.tvEndTimePicker.visibility = View.VISIBLE

                        }
                    }
                }
            }
        }

        binding.tvStartDatePicker.setOnClickListener {
            showDatePickerDialog(isStartDate = true)
        }
        binding.tvEndDatePicker.setOnClickListener {
            showDatePickerDialog(isStartDate = false)
        }
        binding.tvStartTimePicker.setOnClickListener {
            showTimePickerDialog(isStartTime = true)
        }
        binding.tvEndTimePicker.setOnClickListener {
            showTimePickerDialog(isStartTime = false)
        }
        binding.tInEdtEventName.addTextChangedListener {
            viewModel.updateTitle(it.toString())
        }
        binding.tInEdtEventNote.addTextChangedListener {
            viewModel.updateDescription(it.toString())
        }
        binding.switchAllDay.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.updateAllDayStatus(isChecked)
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var bindingDatePicker: DialogItemDatePickerBinding? = null

    @SuppressLint("InflateParams")
    fun showDatePickerDialog(isStartDate: Boolean) {
        var selectedEpochTime: Long = Calendar.getInstance().timeInMillis // Default to current date

        val dialogView = layoutInflater.inflate(R.layout.dialog_item_date_picker, null)
        bindingDatePicker = DialogItemDatePickerBinding.bind(dialogView)

        val datePicker = bindingDatePicker?.datePicker
        val btnOkay = bindingDatePicker?.mBtnOky
        val btnCancel = bindingDatePicker?.mBtnCancel

        // Get the current date
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-based
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Initialize DatePicker with the current date
        datePicker?.init(currentYear, currentMonth, currentDay) { _, year, month, day ->
            val selectedDate = "$day/${month + 1}/$year" // Month is 0-based
            Log.d(TAG, "Selected Date: $selectedDate")
        }
        // Programmatically set a date (e.g., January 1, 2025)
        //datePicker.updateDate(2025, 0, 1)

        // Create and display the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent) // Set your background drawable here

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(true)

        // Handle "OK" button click
        btnOkay?.setOnClickListener {
            // Fetch selected date
            val selectedYear = datePicker?.year ?: currentYear
            val selectedMonth = datePicker?.month ?: currentMonth // 0-based
            val selectedDay = datePicker?.dayOfMonth ?: currentDay

            // Convert to epoch time
            val selectedCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                set(Calendar.MILLISECOND, 0) // Reset milliseconds
            }.timeInMillis
            selectedEpochTime = selectedCalendar

            val selectedDate = "$selectedDay/$selectedMonth/$selectedYear"
            Log.d(TAG, "Final Selected Date (Epoch): $selectedEpochTime, Date: $selectedDate ")

            // Update ViewModel based on start or end date
            if (isStartDate) {
                viewModel.updateStartDate(selectedEpochTime)
            } else {
                viewModel.updateEndDate(selectedEpochTime)
            }

            dialog.dismiss()
        }

        // Handle "Cancel" button click
        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private var bindingTimePicker: DialogItemTimePickerBinding? = null
    @SuppressLint("InflateParams")
    fun showTimePickerDialog(isStartTime: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_item_time_picker,null)
        bindingTimePicker = DialogItemTimePickerBinding.bind(dialogView)

        val timePicker = bindingTimePicker?.timePicker
        val btnOky = bindingTimePicker?.mBtnOky
        val btnCancel = bindingTimePicker?.mBtnCancel

        // Configure TimePicker
        timePicker?.setIs24HourView(false) // Use 24-hour format

        // Create and display the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setBackgroundDrawableResource(android.R.drawable.screen_background_light_transparent) // Set your background drawable here

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(true)

        // Handle "OK" button click
        btnOky?.setOnClickListener {

            val hour = timePicker?.hour ?: 0
            val minute = timePicker?.minute ?: 0

            // Combine selected hour and minute with the current date
            val selectedTimeInMillis = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            Log.d(TAG,"TimePicker: Selected Time in Millis: $selectedTimeInMillis")
            val selectedTime = String.format("%02d:%02d", hour, minute)
            Log.d(TAG,"TimePicker: Selected Time: $selectedTime")

            if (isStartTime) {
                viewModel.updateStartTime(selectedTimeInMillis)
            } else {
                viewModel.updateEndTime(selectedTimeInMillis)
            }


            dialog.dismiss()
        }

        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}