package com.hardik.calendarapp.presentation.ui.new_event

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT
import com.hardik.calendarapp.common.Constants.KEY_EVENT_ALERT
import com.hardik.calendarapp.common.Constants.KEY_EVENT_REPEAT
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.data.database.entity.RepeatOptionConverter
import com.hardik.calendarapp.databinding.DialogItemDatePickerBinding
import com.hardik.calendarapp.databinding.DialogItemEventAlertBinding
import com.hardik.calendarapp.databinding.DialogItemEventCustomAlertMinuteBinding
import com.hardik.calendarapp.databinding.DialogItemEventRepeatBinding
import com.hardik.calendarapp.databinding.DialogItemTimePickerBinding
import com.hardik.calendarapp.databinding.FragmentNewEventBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.TIME_FORMAT_HH_mm
import com.hardik.calendarapp.utillities.DateUtil.TIME_FORMAT_hh_mm_a
import com.hardik.calendarapp.utillities.DateUtil.splitTimeString
import com.hardik.calendarapp.utillities.MyNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class NewEventFragment : Fragment(R.layout.fragment_new_event) {
    private val TAG = BASE_TAG + NewEventFragment::class.java.simpleName

    private val viewModel: NewEventViewModel  by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var argEvent:Event

    var is24HourFormat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: ")
        arguments?.let {
            argEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(KEY_EVENT, Event::class.java)
                    ?: throw IllegalArgumentException("Event is missing")
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(KEY_EVENT)
                    ?: throw IllegalArgumentException("Event is missing")
            }
        }

        if (arguments?.containsKey(KEY_EVENT) == true){
            Log.e(TAG, "onViewCreated: argEvent:$argEvent", )
            populateEventData(event = argEvent)
            updateToolbarTitle(resources.getString(R.string.update_event))
        }else{
            Log.e(TAG, "onViewCreated: argEvent is null", )
            viewModel.resetEventState()
            updateToolbarTitle(resources.getString(R.string.new_event))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) { super.onActivityCreated(savedInstanceState) }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: ")
        _binding = FragmentNewEventBinding.bind(view)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        is24HourFormat = sharedPreferences.getBoolean("time_format", false)


        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.yearState.collectLatest { year ->
                        Log.i(TAG, "setupUI: year:$year")
                        //Toast.makeText(requireContext(), "$year", Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.startDate.collectLatest { startDate ->
                        binding.tvStartDatePicker.text = DateUtil.longToString(
                            timestamp = startDate,
                            pattern = DateUtil.DATE_FORMAT_dd_MMM_yyyy
                        )
                    }
                }
                launch {
                    viewModel.endDate.collectLatest { endDate ->
                        binding.tvEndDatePicker.text = DateUtil.longToString(
                            timestamp = endDate,
                            pattern = DateUtil.DATE_FORMAT_dd_MMM_yyyy
                        )
                    }
                }
                launch {
                    viewModel.startTime.collectLatest { startTime ->
                        binding.tvStartTimePicker.text = DateUtil.longToString(
                            timestamp = startTime,
                            pattern = TIME_FORMAT_HH_mm.takeIf { is24HourFormat } ?:TIME_FORMAT_hh_mm_a
                        )
                    }
                }
                launch {
                    viewModel.endTime.collectLatest { endTime ->
                        binding.tvEndTimePicker.text = DateUtil.longToString(
                            timestamp = endTime,
                            pattern = TIME_FORMAT_HH_mm.takeIf { is24HourFormat } ?:TIME_FORMAT_hh_mm_a
                        )
                    }
                }
                launch {
                    viewModel.isAllDay.collectLatest {isAllDay ->
                        binding.switchAllDay.isChecked = isAllDay

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
                launch {
                    viewModel.title.collectLatest { title ->
                        if (binding.edtEventName.text.toString() != title) {
                            binding.edtEventName.setText(title) // Update UI if needed
                        }
                    }
                }
                launch {
                    viewModel.description.collectLatest{ description ->
                        if (binding.edtEventNote.text.toString()!= description) {
                            binding.edtEventNote.setText(description) // Update UI if needed
                        }
                    }
                }
                launch {
                    viewModel.repeatOption.collectLatest { value: RepeatOption ->
                        if (binding.tvRepeatPicker.text.toString() != value.toString()){
                            binding.tvRepeatPicker.text = RepeatOptionConverter.toDisplayString(context = requireContext(), repeatOption = value)
                        }
                    }
                }
                launch {
                    viewModel.alertOffset.collectLatest { value: AlertOffset ->
                        if (binding.tvAlertPicker.text.toString() != value.toString()){
                            if (value == AlertOffset.BEFORE_CUSTOM_TIME){
                                launch {
                                    viewModel.customAlertOffset.collectLatest { value: Long? ->
                                        val minutesTime = if (value == null) "Custom time is not set"//"null"
                                        else {
                                            //DateUtil.longToString(timestamp = it, pattern = DateUtil.DATE_TIME_FORMAT_yyyy_MM_dd_HH_mm)
                                            "Before ${DateUtil.timestampToMinutes(milliseconds = value)} minute"
                                        }
                                        binding.tvAlertPicker.text = minutesTime
                                    }
                                }
                            }else{
                                binding.tvAlertPicker.text = AlertOffsetConverter.toDisplayString(context = requireContext(), alertOffset = value)
                            }
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
        binding.edtEventName.addTextChangedListener { text ->
            text?.let {
                if (viewModel.title.value != it.toString()) {
                    viewModel.updateTitle(it.toString()) // Update ViewModel state
                }
            }
        }
        binding.tvRepeat.setOnClickListener { navigateToRepeatOptionFrag() ; //showRepetitionDialog()
        }
        binding.tvAlert.setOnClickListener { navigateToAlertOptionFrag(); //showAlertRemindDialog()
        }
        binding.edtEventNote.addTextChangedListener { viewModel.updateDescription(it.toString()) }
        binding.switchAllDay.setOnCheckedChangeListener { buttonView, isChecked -> viewModel.updateAllDayStatus(isChecked) }

        /** Save Event  */
        (activity as MainActivity).binding.appBarMain.saveEventIcon.apply {
            text = resources.getString(R.string.action_save)
            setOnClickListener {
                lifecycleScope.launch {
                    val msg: String = viewModel.run {
                        val id = if (arguments?.containsKey(KEY_EVENT) == true) argEvent.id else null
                        val eventId = if (arguments?.containsKey(KEY_EVENT) == true) argEvent.eventId else 0
                        Log.e(TAG, "eventId is -> id: $id", )
                        //AlarmScheduler.cancelAlarm(requireContext(), eventId.toInt())
                        if (id != null) { viewModel.cancelAlarm(id) }
                        insertCustomEvent(context = requireContext(),id = id)
                    }

                    // Display a message to the user
                    //Snackbar.make(view, msg, Snackbar.LENGTH_LONG).setAnchorView(binding.baseline).show()
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                    // Reset the fields after successful insertion
                    if (msg == Constants.EVENT_INSERT_SUCCESSFULLY || msg == Constants.EVENT_UPDATE_SUCCESSFULLY) {
                        viewModel.resetEventState()
                    }
                    findNavController().popBackStack(R.id.newEventFragment.takeIf { Constants.EVENT_INSERT_SUCCESSFULLY == msg }?: R.id.viewEventFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                    //findNavController().popBackStack(R.id.newEventFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView: ")
        _binding = null
    }

    private fun populateEventData(event: Event) {
        Log.e(TAG, "populateEventData: ${DateUtil.isAllDay(startTime = event.startTime, endTime = event.endTime)}", )
        // Update ViewModel with the data
        viewModel.updateTitle(event.title)
        viewModel.updateDescription(event.description)
        viewModel.updateStartDate(DateUtil.stringToLong(event.startDate))
        viewModel.updateEndDate(DateUtil.stringToLong(event.endDate))
        viewModel.updateStartTime(event.startTime)
        viewModel.updateEndTime(event.endTime)
        viewModel.updateAllDayStatus(DateUtil.isAllDay(startTime = event.startTime, endTime = event.endTime))//event.isAllDay)
        viewModel.updateRepeatOption(event.repeatOption)
        viewModel.updateAlertOffset(event.alertOffset)
        viewModel.updateCustomAlertOffset(event.customAlertOffset)
    }


    private var bindingDatePicker: DialogItemDatePickerBinding? = null
    @SuppressLint("InflateParams")
    fun showDatePickerDialog(isStartDate: Boolean) {
        var selectedEpochTime: Long = Calendar.getInstance().timeInMillis // Default to current date

        val dialogView = layoutInflater.inflate(R.layout.dialog_item_date_picker, null)
        bindingDatePicker = DialogItemDatePickerBinding.bind(dialogView)

        val datePicker = bindingDatePicker?.datePicker
        val btnOkay = bindingDatePicker?.btnDone
        val btnCancel = bindingDatePicker?.btnCancel

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
        // Programmatically set a date (e.g., January 1, 2025) datePicker.updateDate(2025,0,1)
        if (arguments?.containsKey(KEY_EVENT) == true){
            val data = DateUtil.stringToDateTriple(argEvent.startDate.takeIf { isStartDate } ?: argEvent.endDate,)
            datePicker?.updateDate(data.first.toInt(), data.second.toInt(), data.third.toInt())
        }

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
        val btnOky = bindingTimePicker?.btnDone
        val btnCancel = bindingTimePicker?.btnCancel

        // Configure TimePicker
        timePicker?.apply {
            //setIs24HourView(false) // Use 12-hour format
            setIs24HourView(is24HourFormat) // Use 12-hour format
            //hour = 0 // Set the hour (0 for 12 AM)
            //minute = 23 // Set the minute
            // Programmatically set a time (e.g., 0:12)
            if (arguments?.containsKey(KEY_EVENT) == true){
                val data = DateUtil.longToString(timestamp = argEvent.startTime.takeIf { isStartTime } ?: argEvent.endTime,pattern = DateUtil.TIME_FORMAT_hh_mm_a)

                // Split the time string into hour, minute, and AM/PM
                val time = splitTimeString(data)
                val hour = time.first.toInt()
                val minute = time.second.toInt()
                val amPm = time.third

                // Set the hour and minute
                this.hour = if (amPm == "PM" && hour != 12) {
                    hour + 12 // Convert PM hours (except 12 PM) to 24-hour format
                } else if (amPm == "AM" && hour == 12) {
                    0 // Convert 12 AM to 0 hours (midnight)
                } else {
                    hour
                }
                this.minute = minute
            }
        }

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

    private fun updateToolbarTitle(title: String) {
        // Dynamically update NavDestination label
        //findNavController().currentDestination?.label = title
        mainViewModel.updateToolbarTitle(title ?: resources.getString(R.string.app_name))
    }

    private var dialogItemRepeatBinding: DialogItemEventRepeatBinding? = null
    private fun showRepetitionDialog() {
        Log.d(TAG, "showRepetitionDialog: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_item_event_repeat, null)
        dialogItemRepeatBinding = DialogItemEventRepeatBinding.bind(dialogView)

        // Initialize Repetition with default
        val selectedRepeatOption = RepeatOptionConverter.toDisplayString(context = requireContext(), repeatOption = viewModel.repeatOption.value)

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

        /*dialogItemRepeatBinding?.dialogItemEventRepeatNone?.apply {
            if (selectedRepeatOption.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateRepeatOption(RepeatOption.NONE)
                dialog.dismiss()
            }
        }*/
        /*dialogItemRepeatBinding?.dialogItemEventRepeatAtOnce?.apply {
            if (selectedRepeatOption.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateRepeatOption(RepeatOption.ONCE)
                dialog.dismiss()
            }
        }*/
        dialogItemRepeatBinding?.dialogItemEventRepeatNever?.apply {
            if (selectedRepeatOption.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateRepeatOption(RepeatOption.NEVER)
                dialog.dismiss()
            }
        }

        dialogItemRepeatBinding?.dialogItemEventRepeatEveryDay?.apply {
            if (selectedRepeatOption.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateRepeatOption(RepeatOption.DAILY)
                dialog.dismiss()
            }
        }
        dialogItemRepeatBinding?.dialogItemEventRepeatEveryWeek?.apply {
            if (selectedRepeatOption.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateRepeatOption(RepeatOption.WEEKLY)
                dialog.dismiss()
            } }
        dialogItemRepeatBinding?.dialogItemEventRepeatEveryMonth?.apply {
            if (selectedRepeatOption.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateRepeatOption(RepeatOption.MONTHLY)
                dialog.dismiss()
            }
        }
        dialogItemRepeatBinding?.dialogItemEventRepeatEveryYear?.apply {
            if (selectedRepeatOption.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateRepeatOption(RepeatOption.YEARLY)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private var dialogItemEventAlertBinding: DialogItemEventAlertBinding? = null
    private fun showAlertRemindDialog(){
        Log.d(TAG, "showAlertRemindDialog: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_item_event_alert, null)
        dialogItemEventAlertBinding = DialogItemEventAlertBinding.bind(dialogView)

        // Initialize AlertOffset option default
        val selectedAlertOffsetOption = AlertOffsetConverter.toMilliseconds(alertOffset = viewModel.alertOffset.value)
        val selectedAlertOffsetOptionText = AlertOffsetConverter.toDisplayString(context = requireContext(), alertOffset = viewModel.alertOffset.value)

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

        dialogItemEventAlertBinding?.dialogItemEventAlertNone?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.NONE)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertAtTime?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.AT_TIME)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore5Min?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_5_MINUTES)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore10Min?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_10_MINUTES)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore15Min?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_15_MINUTES)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore30Min?.apply {
            if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_30_MINUTES)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Hour?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_1_HOUR)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore12Hours?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_12_HOURS)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Day?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_1_DAY)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore3Day?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_3_DAYS)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore5Day?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_5_DAYS)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Week?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_1_WEEK)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore2Weeks?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_2_WEEKS)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Month?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_1_MONTH)
                dialog.dismiss()
            }
        }
        dialogItemEventAlertBinding?.dialogItemEventAlertBeforeCustomTime?.apply {
            //if (selectedAlertOffsetOptionText.equals(this.text.toString(), ignoreCase = true)) this.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.checked),  null)
            setOnClickListener {
                //viewModel.updateAlertOffset(AlertOffset.BEFORE_CUSTOM_TIME)
                viewModel.updateAlertOffset(viewModel.alertOffset.value)
                //todo: open custom time set
                showCustomTimePickerDialog()
                dialog.dismiss()
            }
        }
        // Update the checked state based on the selected alert offset
        clearAndSetChecked(viewModel.alertOffset.value)

        dialog.show()
    }

    private fun clearAndSetChecked(alertOffset: AlertOffset) {
        Log.i(TAG, "clearAndSetChecked: $alertOffset")
        // Clear all compound drawables
        dialogItemEventAlertBinding?.apply {
            // Clear all checked states
            dialogItemEventAlertNone.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertAtTime.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore5Min.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore10Min.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore15Min.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore30Min.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore1Hour.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore12Hours.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore1Day.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore3Day.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore5Day.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore1Week.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore2Weeks.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBefore1Month.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            dialogItemEventAlertBeforeCustomTime.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        // Set checked state for the selected alert offset
        val checkedDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.checked)
        when (alertOffset) {
            AlertOffset.NONE -> dialogItemEventAlertBinding?.dialogItemEventAlertNone?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.AT_TIME -> dialogItemEventAlertBinding?.dialogItemEventAlertAtTime?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_5_MINUTES -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore5Min?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_10_MINUTES -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore10Min?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_15_MINUTES -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore15Min?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_30_MINUTES -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore30Min?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_1_HOUR -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Hour?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_12_HOURS -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore12Hours?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_1_DAY -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Day?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_3_DAYS -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore3Day?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_5_DAYS -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore5Day?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_1_WEEK -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Week?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_2_WEEKS -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore2Weeks?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_1_MONTH -> dialogItemEventAlertBinding?.dialogItemEventAlertBefore1Month?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
            AlertOffset.BEFORE_CUSTOM_TIME -> dialogItemEventAlertBinding?.dialogItemEventAlertBeforeCustomTime?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
        }
    }

    private var dialogItemEventCustomAlertMinuteBinding: DialogItemEventCustomAlertMinuteBinding? = null
    private fun showCustomTimePickerDialog(){
        Log.d(TAG, "showCustomTimePickerDialog: ")
        val dialogView = layoutInflater.inflate(R.layout.dialog_item_event_custom_alert_minute, null)
        dialogItemEventCustomAlertMinuteBinding = DialogItemEventCustomAlertMinuteBinding.bind(dialogView)

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

        dialogItemEventCustomAlertMinuteBinding?.apply {

            var customAlertOffsetTimeStamp: Long? = null
            edtEventCustomAlertMinute.addTextChangedListener { text ->
                //customAlertOffsetTimeStamp = if(text == null) null
                //else DateUtil.minutesToTimestamp(text.toString().toInt())
                try {
                    val number = text.toString().toInt()
                    // Use the number
                    customAlertOffsetTimeStamp = DateUtil.minutesToTimestamp(number)
                } catch (e: NumberFormatException) {
                    Log.e("NewEventFragment", "Error parsing input", e)
                    edtEventCustomAlertMinute.error = "Please enter a valid number"
                }
            }

            btnDone.setOnClickListener {
                viewModel.updateAlertOffset(AlertOffset.BEFORE_CUSTOM_TIME)

                if(viewModel.customAlertOffset.value != customAlertOffsetTimeStamp){
                    viewModel.updateCustomAlertOffset(customAlertOffset = customAlertOffsetTimeStamp)// Update ViewModel state
                }

                dialog.dismiss()
            }
            btnCancel.setOnClickListener {
                viewModel.updateAlertOffset(viewModel.alertOffset.value)
                dialog.dismiss()
                showAlertRemindDialog()
            }
        }

        dialog.show()
    }

    private fun navigateToRepeatOptionFrag() {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            Log.e(TAG, "navigateToRepeatOptionFrag():  ${Thread.currentThread().name}", )
            val repeatOpt: String = RepeatOptionConverter.toDisplayString(requireContext(), viewModel.repeatOption.value)
            val bundle = Bundle().apply {
                putString(KEY_EVENT_REPEAT,  repeatOpt)
            }

            findNavController().navigate(R.id.repeatOptionFragment, bundle, MyNavigation.navOptions)
        }
    }

    private fun navigateToAlertOptionFrag() {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            Log.e(TAG, "navigateToAlertOptionFrag():  ${Thread.currentThread().name}", )
            val alertOpt: String = AlertOffsetConverter.toDisplayString(requireContext(), viewModel.alertOffset.value)
            val bundle = Bundle().apply { putString(KEY_EVENT_ALERT, alertOpt) }
            findNavController().navigate(R.id.alertOptionFragment, bundle, MyNavigation.navOptions)
        }
    }
}