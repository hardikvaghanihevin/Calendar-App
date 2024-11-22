package com.hardik.calendarapp.presentation.ui.home

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.FragmentHomeBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.presentation.adapter.EventAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private final val TAG = BASE_TAG + HomeFragment::class.java.simpleName

    private var _binding: FragmentHomeBinding? = null
    private lateinit var eventAdapter: EventAdapter
    lateinit var mainViewModel: MainViewModel

    /** This property is only valid between [onCreateView] and [onDestroyView].
     */
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        //val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //homeViewModel.text.observe(viewLifecycleOwner) { binding.textView.text = it     }
        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
// binding.customView.addEvent("2024-11-25", Event(title = "Independence Day", startTime = 0L, endTime = 0L, startDate = "", endDate = "", eventType =   EventType.NATIONAL_HOLIDAY))
// binding.customView.incrementMonth()

