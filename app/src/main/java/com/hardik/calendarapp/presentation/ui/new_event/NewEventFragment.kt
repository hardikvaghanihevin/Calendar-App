package com.hardik.calendarapp.presentation.ui.new_event

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT_ALERT
import com.hardik.calendarapp.common.Constants.KEY_EVENT_JSON
import com.hardik.calendarapp.common.Constants.KEY_EVENT_REPEAT
import com.hardik.calendarapp.common.Constants.PREF_KEY_TIME_FORMAT
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.data.database.entity.RepeatOptionConverter
import com.hardik.calendarapp.databinding.DialogItemDatePickerBinding
import com.hardik.calendarapp.databinding.DialogItemTimePickerBinding
import com.hardik.calendarapp.databinding.FragmentNewEventBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.DateUtil.TIME_FORMAT_HH_mm
import com.hardik.calendarapp.utillities.DateUtil.TIME_FORMAT_hh_mm_a
import com.hardik.calendarapp.utillities.DateUtil.splitTimeString
import com.hardik.calendarapp.utillities.GsonUtil
import com.hardik.calendarapp.utillities.KeyboardUtils
import com.hardik.calendarapp.utillities.MyNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class NewEventFragment : Fragment() {
    private val TAG = BASE_TAG + NewEventFragment::class.java.simpleName

    private val viewModel: NewEventViewModel  by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var argEvent:Event

    var is24HourFormat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val eventJson = it.getString(KEY_EVENT_JSON)
            eventJson?.let {
                argEvent = GsonUtil.fromJson(eventJson, Event::class.java)!!
            }
        }

        if (arguments?.containsKey(KEY_EVENT_JSON) == true){
            populateEventData(event = argEvent)
            updateToolbarTitle(resources.getString(R.string.update_event))
        }else{
            viewModel.resetEventState()
            updateToolbarTitle(resources.getString(R.string.new_event))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        is24HourFormat = sharedPreferences.getBoolean(PREF_KEY_TIME_FORMAT, false)


        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

                        //here set time and date according to start date or end date
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
                                        val minutesTime = if (value == null) getString(R.string.custom_time_is_not_set)
                                        else {
                                            "${DateUtil.timestampToMinutes(milliseconds = value)} " + resources.getString(R.string.minutes_before)
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
        binding.tvRepeat.setOnClickListener { navigateToRepeatOptionFrag() }
        binding.tvAlert.setOnClickListener { navigateToAlertOptionFrag() }
        binding.edtEventNote.addTextChangedListener { viewModel.updateDescription(it.toString()) }
        binding.switchAllDay.apply {
           setOnCheckedChangeListener { buttonView, isChecked -> viewModel.updateAllDayStatus(isChecked) }
        }

        /** Save Event  */
        (activity as MainActivity).run {

            this.binding.appBarMain.includedAppBarMainCustomToolbar.includedNewEvent.includedSave.root.apply {
                text = resources.getString(R.string.action_save)

                val sClick = {
                    try {
                        val mainActivity = requireActivity() as MainActivity

                        lifecycleScope.launch {
                            val msg: String = viewModel.run {
                                val id =
                                    if (arguments?.containsKey(KEY_EVENT_JSON) == true) argEvent.id else null

                                if (id != null) {
                                    viewModel.cancelAlarm(event = argEvent)
                                }
                                insertCustomEvent(context = requireContext(), id = id)
                            }

                            // Display a message to the user
                            val notifyUser =
                                context.resources.getString(R.string.event_insert_successfully)
                                    .takeIf { msg == Constants.EVENT_INSERT_SUCCESSFULLY }
                                    ?: context.resources.getString(R.string.event_update_successfully)
                                        .takeIf { msg == Constants.EVENT_UPDATE_SUCCESSFULLY }
                                    ?: msg

                            if (isAdded) {// Ensure fragment is attached before accessing view
                                Snackbar.make(view, notifyUser, Snackbar.LENGTH_SHORT).show()
                            }

                            // Reset the fields after successful insertion
                            if (msg == Constants.EVENT_INSERT_SUCCESSFULLY || msg == Constants.EVENT_UPDATE_SUCCESSFULLY) {
                                viewModel.resetEventState()

                                if (mainViewModel.isComingFromNotification.value) {
                                    mainActivity.navigateToYearView()
                                    mainViewModel.setIsComingFromNotification(isComing = false)
                                } else {
                                    findNavController().popBackStack(R.id.newEventFragment.takeIf { Constants.EVENT_INSERT_SUCCESSFULLY == msg }
                                        ?: R.id.viewEventFragment,
                                        inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                                }
                            }
                        }

                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "NewEventFragment: save event: Activity is not attached", e)
                    }
                }

                // Always set the click listener
                setOnClickListener {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permissionNotification = Manifest.permission.POST_NOTIFICATIONS
                        if (this@run.permissionManager.checkPermission(permissionNotification)) {
                            sClick()
                        } else {
                            this@run.showNotificationPermissionDialog {
                                if (this@run.permissionManager.checkPermission(permissionNotification))
                                    sClick() // Call `sClick()` only after permission is granted, on next click
                            }
                        }
                    } else {
                        sClick()
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        // Temporarily set adjustResize for this fragment
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onPause() {
        super.onPause()
        // Reset to adjustNothing when leaving this fragment
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        KeyboardUtils.hideKeyboard(requireActivity())
        _binding = null
    }

    private fun populateEventData(event: Event) {
        // Update ViewModel with the data
        viewModel.updateTitle(event.title)
        viewModel.updateDescription(event.description)
        viewModel.updateStartDate(DateUtil.stringToLong(event.startDate))
        viewModel.updateEndDate(DateUtil.stringToLong(event.endDate))
        viewModel.updateStartTime(event.startTime)
        viewModel.updateEndTime(event.endTime)
        viewModel.updateAllDayStatus(event.isAllDay)
        viewModel.updateSourceType(event.sourceType)
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
        datePicker?.init(currentYear, currentMonth, currentDay) { _, year, month, day -> }

        // Programmatically set a date (e.g., January 1, 2025) datePicker.updateDate(2025,0,1)
        val data = DateUtil.stringToDateTriple(DateUtil.longToString(viewModel.startDate.value).takeIf { isStartDate } ?: DateUtil.longToString(viewModel.endDate.value),)
        datePicker?.updateDate(data.first.toInt(), data.second.toInt(), data.third.toInt())

        // Create and display the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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
            setIs24HourView(is24HourFormat) // Use 12-hour format
            // Programmatically set a time (e.g., 0:12)
            val timeStamp = viewModel.startTime.value.takeIf { isStartTime } ?: viewModel.endTime.value
            //val data = DateUtil.longToString(timestamp = timeStamp, pattern = TIME_FORMAT_hh_mm_a)

            // Split the time string into hour, minute, and AM/PM
            val time = splitTimeString(timeStamp)
            val hour = time.first.toInt()
            val minute = time.second.toInt()
            val amPm = time.third.trim()


            // Get localized AM/PM strings
            val symbols = DateFormatSymbols(Locale.getDefault())
            val amPmSystem = symbols.amPmStrings // ["AM", "PM"]
            val amString = amPmSystem[0] // Localized "AM"
            val pmString = amPmSystem[1] // Localized "PM"

            // Determine correct hour format
            val hourToSet = when {
                is24HourFormat -> { // Convert 12-hour to 24-hour if needed
                    when {
                        amPm == pmString && hour != 12 -> hour + 12 // PM but not 12 PM
                        amPm == amString && hour == 12 -> 0 // 12 AM -> 00:00
                        else -> hour
                    }
                }
                else -> { // Keep it in 12-hour format
                    when {
                        amPm == pmString && hour != 12 -> hour + 12 // PM but not 12 PM
                        amPm == amString && hour == 12 -> 0 // 12 AM -> 00:00
                        else -> hour
                    }
                }
            }
            //Log.e(TAG, "showTimePickerDialog: time: $time | hour: $hour | minute: $minute | amPm: $amPm | Converted Hour: $hourToSet")
            // Set the values in TimePicker
            this.hour = hourToSet
            this.minute = minute

        }

        // Create and display the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Set background to transparent if needed
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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

            val endTimeInMillis = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            if (isStartTime) {
                viewModel.updateStartTime(selectedTimeInMillis)
            } else {
                viewModel.updateEndTime(endTimeInMillis)
            }


            dialog.dismiss()
        }

        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateToolbarTitle(title: String) {
        mainViewModel.updateToolbarTitle(title ?: resources.getString(R.string.app_name))
    }

    private fun navigateToRepeatOptionFrag() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Make sure the navigation happens on the main thread
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
            val alertOpt: String = AlertOffsetConverter.toDisplayString(requireContext(), viewModel.alertOffset.value)
            val bundle = Bundle().apply { putString(KEY_EVENT_ALERT, alertOpt) }

            findNavController().navigate(R.id.alertOptionFragment, bundle, MyNavigation.navOptions)
        }
    }
}