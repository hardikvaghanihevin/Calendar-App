package com.hardik.calendarapp.presentation.ui.alert_option

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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.common.Constants.KEY_EVENT_ALERT
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.AlertOffsetConverter
import com.hardik.calendarapp.databinding.DialogItemEventCustomAlertMinuteBinding
import com.hardik.calendarapp.databinding.FragmentAlertOptionBinding
import com.hardik.calendarapp.presentation.adapter.AlertOptionAdapter
import com.hardik.calendarapp.presentation.adapter.AlertOptionItem
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.new_event.NewEventViewModel
import com.hardik.calendarapp.utillities.DateUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class AlertOptionFragment : Fragment(R.layout.fragment_alert_option) {
    private val TAG = Constants.BASE_TAG + AlertOptionFragment::class.java.simpleName

    private val viewModel: NewEventViewModel by activityViewModels()

    private var _binding: FragmentAlertOptionBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertOffsetValues: Array<String>
    private var selectedAlertOffset: AlertOffset? = null

    // Adapter and items
    private lateinit var alertOffsetAdapter: AlertOptionAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        arguments?.let {
            val argAlertOffset = it.getString(KEY_EVENT_ALERT)
            selectedAlertOffset = argAlertOffset?.let { it1 -> AlertOffsetConverter.fromDisplayString(requireContext(), argAlertOffset) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { return inflater.inflate(R.layout.fragment_alert_option, container, false) }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlertOptionBinding.bind(view)

        // Load string arrays
        alertOffsetValues = resources.getStringArray(R.array.alert_offset_options)

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.alertOffset.collectLatest { aOffset: AlertOffset -> selectedAlertOffset = aOffset }
            }
        }

        val alertOffsetOpt: String? = selectedAlertOffset?.let { AlertOffsetConverter.toDisplayString(context = requireContext(), alertOffset =  it) }
        // Prepare alertOffset option items
        val alertOffsetItems = alertOffsetValues.mapIndexed { index, alertOffset: String ->
            AlertOptionItem(alertOffset, alertOffset.equals(alertOffsetOpt, ignoreCase = true))
        }
        // Set up Recyclerview
        alertOffsetAdapter = AlertOptionAdapter(requireContext(), alertOffsetItems) { postion ->
            selectedAlertOffset = AlertOffsetConverter.fromDisplayString(requireContext(), alertOffsetValues[postion])
            if (selectedAlertOffset == AlertOffset.BEFORE_CUSTOM_TIME){
                Log.i(TAG, "onViewCreated: AlertOffset.BEFORE_CUSTOM_TIME`")
                showCustomTimePickerDialog()
            }
        }

        binding.alertOptionRecView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertOffsetAdapter
        }

        /** Save Selected Language */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.saveSelectLanguageIcon.setOnClickListener {
            if (isAdded) {
                lifecycleScope.launch {
                    selectedAlertOffset?.let { it1 -> viewModel.updateAlertOffset(it1) }
                    findNavController().popBackStack(R.id.alertOptionFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
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
                    if (number in 0..59) { // The number is between 0 and 59
                        Log.i(TAG, "showCustomTimePickerDialog: The number is in the range of 0 to 59.")
                        // Use the number
                        customAlertOffsetTimeStamp = DateUtil.minutesToTimestamp(number)

                    } else { // The number is outside the range
                        Log.i(TAG, "showCustomTimePickerDialog: The number is not in the range of 0 to 59.")
                        Toast.makeText(requireContext(),"The range between 0 to 59", Toast.LENGTH_SHORT).show()
                        viewModel.updateAlertOffset(viewModel.alertOffset.value)
                    }
                } catch (e: NumberFormatException) {
                    Log.e("NewEventFragment", "Error parsing input", e)
                    edtEventCustomAlertMinute.error = "Please enter a valid number"
                }
            }

            btnDone.setOnClickListener {

                if(viewModel.customAlertOffset.value != customAlertOffsetTimeStamp){
                    viewModel.updateAlertOffset(AlertOffset.BEFORE_CUSTOM_TIME)
                    viewModel.updateCustomAlertOffset(customAlertOffset = customAlertOffsetTimeStamp)// Update ViewModel state
                }

                dialog.dismiss()
            }
            btnCancel.setOnClickListener {
                viewModel.updateAlertOffset(viewModel.alertOffset.value)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

}