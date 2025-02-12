package com.hardik.calendarapp.presentation.ui.alert_option

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
                viewModel.alertOffset.collectLatest { aOffset: AlertOffset ->
                    selectedAlertOffset = aOffset

                    if (aOffset == AlertOffset.BEFORE_CUSTOM_TIME){
                        launch {
                            viewModel.customAlertOffset.collectLatest { value: Long? ->
                                val minutesTime = if (value == null) -1
                                else {
                                    DateUtil.timestampToMinutes(milliseconds = value)
                                }
                                alertOffsetAdapter.updateCustomTime(minutesTime)
                            }
                        }
                    }
                }
            }
        }

        val alertOffsetOpt: String? = selectedAlertOffset?.let { AlertOffsetConverter.toDisplayString(context = requireContext(), alertOffset =  it) }
        // Prepare alertOffset option items
        val alertOffsetItems = alertOffsetValues.mapIndexed { index, alertOffset: String ->
            AlertOptionItem(alertOffset, alertOffset.equals(alertOffsetOpt, ignoreCase = true))
        }
        // Set up Recyclerview
        alertOffsetAdapter = AlertOptionAdapter(
            requireContext(),
            alertOffsetItems,
            onItemSelected = { position ->
                selectedAlertOffset = AlertOffsetConverter.fromDisplayString( requireContext(), alertOffsetValues[position] ) },
            onCustomTimeSelected = { ->
                // Open custom time dialog
                showCustomTimePickerDialog()
            }
        )

        binding.alertOptionRecView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)

            val margin = resources.getDimension(R.dimen.itemRepeatAlertVerticalSpacing_dev2).toInt()

            addItemDecoration(object: RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view) // Get the position of the item
                    val itemCount = parent.adapter?.itemCount ?: 0

                    if (position == RecyclerView.NO_POSITION) return

                    // Apply margin adjustments
                    when (position) {
                        0 -> { // First item
                            outRect.top = 0//margin
                            outRect.bottom = margin
                        }
                        itemCount - 1 -> { // Last item
                            outRect.top = margin
                            outRect.bottom = margin * 2 //0
                        }
                        else -> { // Middle items
                            outRect.top = margin
                            outRect.bottom = margin
                        }
                    }
                }
            })

            adapter = alertOffsetAdapter
        }

        /** Save Selected Icon */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedAlertOption.includedSaveSelect.root.setOnClickListener {
            if (isAdded) {
                lifecycleScope.launch {
                    selectedAlertOffset?.let { it1 -> viewModel.updateAlertOffset(it1) }
                    findNavController().popBackStack(R.id.alertOptionFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
        }
    }

    private var dialogItemEventCustomAlertMinuteBinding: DialogItemEventCustomAlertMinuteBinding? = null
    @SuppressLint("NotifyDataSetChanged")
    private fun showCustomTimePickerDialog(){
        val dialogView = layoutInflater.inflate(R.layout.dialog_item_event_custom_alert_minute, null)
        dialogItemEventCustomAlertMinuteBinding = DialogItemEventCustomAlertMinuteBinding.bind(dialogView)

        // Create and display the dialog
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        // Set background to transparent if needed
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Ensure the dialog's size wraps the content
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width
                ViewGroup.LayoutParams.WRAP_CONTENT  // Height
            )
        }

        dialog.setCancelable(false)

        dialogItemEventCustomAlertMinuteBinding?.apply {

            val existTime = DateUtil.timestampToMinutes(viewModel.customAlertOffset.value ?: 0)
            edtEventCustomAlertMinute.apply {
                if (existTime == 0 || existTime == -1)
                    hint = "00"
                else
                    setText("$existTime")
            }

            btnDone.setOnClickListener {
                val customTime = edtEventCustomAlertMinute.text.toString().toIntOrNull() ?: 0

                if (customTime in 0..59) { // Check if the value is in the valid range
                    // Update the adapter and ViewModel
                    alertOffsetAdapter.updateCustomTime(customTime)
                    viewModel.updateAlertOffset(AlertOffset.BEFORE_CUSTOM_TIME)
                    val customAlertOffsetTimeStamp = DateUtil.minutesToTimestamp(customTime)
                    viewModel.updateCustomAlertOffset(customAlertOffset = customAlertOffsetTimeStamp)

                    dialog.dismiss()

                } else {
                    // Show a toast if the input is invalid
                    Toast.makeText(requireContext(), getString(R.string.invalid_time_range), Toast.LENGTH_SHORT).show()
                }
            }

            btnCancel.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

}