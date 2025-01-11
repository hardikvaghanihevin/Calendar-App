package com.hardik.calendarapp.presentation.ui.repeat_option

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
import com.hardik.calendarapp.common.Constants.KEY_EVENT_REPEAT
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.data.database.entity.RepeatOptionConverter
import com.hardik.calendarapp.databinding.FragmentRepeatOptionBinding
import com.hardik.calendarapp.presentation.adapter.LanguageAdapter
import com.hardik.calendarapp.presentation.adapter.LanguageItem
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.presentation.ui.new_event.NewEventViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class RepeatOptionFragment : Fragment(R.layout.fragment_repeat_option) {
    private val TAG = Constants.BASE_TAG + RepeatOptionFragment::class.java.simpleName

    private val viewModel: NewEventViewModel by activityViewModels()

    private var _binding: FragmentRepeatOptionBinding? = null
    private val binding get() = _binding!!

    private lateinit var repeatOptionValues: Array<String>
    private var selectedRepeatOption: RepeatOption? = null

    // Adapter and items
    private lateinit var repeatOptionAdapter: LanguageAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val argRepeatOpt = it.getString(KEY_EVENT_REPEAT)
            selectedRepeatOption = argRepeatOpt?.let { it1 -> RepeatOptionConverter.fromDisplayString(requireContext(), it1) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { return inflater.inflate(R.layout.fragment_repeat_option, container, false) }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRepeatOptionBinding.bind(view)

        // Load string arrays
        repeatOptionValues = resources.getStringArray(R.array.repeat_options)

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.repeatOption.collectLatest {rOpt: RepeatOption -> selectedRepeatOption = rOpt }
            }
        }

        val repeatOpt: String? = selectedRepeatOption?.let { RepeatOptionConverter.toDisplayString(context = requireContext(), repeatOption = it) }
        // Prepare repeat option items
        val repeatOptionItems = repeatOptionValues.mapIndexed { index, repeatOption: String ->
            LanguageItem(repeatOption, repeatOption.equals(repeatOpt, ignoreCase = true))
        }
        // Set up Recyclerview
        repeatOptionAdapter = LanguageAdapter(requireContext(), repeatOptionItems) { position ->
            selectedRepeatOption = RepeatOptionConverter.fromDisplayString(requireContext(), repeatOptionValues[position])
        }

        binding.repeatOptionRecView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = repeatOptionAdapter
        }

        /** Save Selected Language */
        (activity as MainActivity).binding.appBarMain.saveSelectLanguageIcon.setOnClickListener {
            if (isAdded){
                lifecycleScope.launch {
                    selectedRepeatOption?.let { it1 -> viewModel.updateRepeatOption(it1) }
                    findNavController().popBackStack(R.id.repeatOptionFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                }
            }
        }
    }

}