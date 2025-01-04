package com.hardik.calendarapp.presentation.alert_option

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
import com.hardik.calendarapp.databinding.FragmentAlertOptionBinding
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.calendar_month.adapter.LanguageAdapter
import com.hardik.calendarapp.presentation.ui.calendar_month.adapter.LanguageItem
import com.hardik.calendarapp.presentation.ui.new_event.NewEventViewModel
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
    private lateinit var alertOffsetAdapter: LanguageAdapter
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
            LanguageItem(alertOffset, alertOffset.equals(alertOffsetOpt, ignoreCase = true))
        }
        // Set up Recyclerview
        alertOffsetAdapter = LanguageAdapter(requireContext(), alertOffsetItems) { postion ->
            selectedAlertOffset = AlertOffsetConverter.fromDisplayString(requireContext(), alertOffsetValues[postion])
        }

        binding.alertOptionRecView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertOffsetAdapter
        }

        /** Save Selected Language */
        (activity as MainActivity).binding.saveSelectLanguageIcon.setOnClickListener {
            if (isAdded) {
                lifecycleScope.launch {
                    selectedAlertOffset?.let { it1 -> viewModel.updateAlertOffset(it1) }
                    findNavController().popBackStack(R.id.alertOptionFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
        }
    }


}