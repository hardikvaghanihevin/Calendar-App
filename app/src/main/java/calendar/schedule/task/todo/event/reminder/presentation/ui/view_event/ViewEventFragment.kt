package calendar.schedule.task.todo.event.reminder.presentation.ui.view_event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import calendar.schedule.task.todo.event.reminder.R
import calendar.schedule.task.todo.event.reminder.common.Constants.BASE_TAG
import calendar.schedule.task.todo.event.reminder.common.Constants.KEY_EVENT_JSON
import calendar.schedule.task.todo.event.reminder.common.Constants.PREF_KEY_TIME_FORMAT
import calendar.schedule.task.todo.event.reminder.data.database.entity.AlertOffset
import calendar.schedule.task.todo.event.reminder.data.database.entity.AlertOffsetConverter
import calendar.schedule.task.todo.event.reminder.data.database.entity.Event
import calendar.schedule.task.todo.event.reminder.data.database.entity.RepeatOptionConverter
import calendar.schedule.task.todo.event.reminder.data.database.entity.SourceType
import calendar.schedule.task.todo.event.reminder.databinding.FragmentViewEventBinding
import calendar.schedule.task.todo.event.reminder.presentation.MainViewModel
import calendar.schedule.task.todo.event.reminder.presentation.ui.MainActivity
import calendar.schedule.task.todo.event.reminder.presentation.ui.new_event.NewEventViewModel
import calendar.schedule.task.todo.event.reminder.utillities.DateUtil
import calendar.schedule.task.todo.event.reminder.utillities.DateUtil.TIME_FORMAT_HH_mm
import calendar.schedule.task.todo.event.reminder.utillities.DisplayUtil
import calendar.schedule.task.todo.event.reminder.utillities.DisplayUtil.hideViewWithAnimation
import calendar.schedule.task.todo.event.reminder.utillities.GsonUtil
import calendar.schedule.task.todo.event.reminder.utillities.MyNavigation.navOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewEventFragment : Fragment() {
    private val TAG = BASE_TAG + ViewEventFragment::class.java.simpleName

    private val viewModel: NewEventViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentViewEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var argEvent: Event

    private var is24HourFormat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val eventJson = it.getString(KEY_EVENT_JSON)
            eventJson?.let {
            argEvent = GsonUtil.fromJson(eventJson, Event::class.java)!!
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentViewEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        arguments?.let {
            val eventJson = it.getString(KEY_EVENT_JSON)
            eventJson?.let {
                argEvent = GsonUtil.fromJson(eventJson, Event::class.java)!!

                argEvent.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        populateEventData(it)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        is24HourFormat = sharedPreferences.getBoolean(PREF_KEY_TIME_FORMAT, false)


        /** Delete Event  */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedViewEvent.includedDelete.root.apply {

            if (arguments?.containsKey(KEY_EVENT_JSON) == true){
                if (argEvent.sourceType != SourceType.CURSOR && argEvent.sourceType != SourceType.LOCAL){
                    hideViewWithAnimation(this)
                }
            }

            text = resources.getString(R.string.action_delete)
            setOnClickListener {
                lifecycleScope.launch {
                    val isDelete = viewModel.deleteEvent(argEvent)
                    if (isDelete == 1) {
                        if (argEvent.sourceType == SourceType.CURSOR){ }//deleteEvent here call back
                    }
                    Snackbar.make(view, resources.getString(R.string.event_deleted), Snackbar.LENGTH_SHORT).show()
                    viewModel.resetEventState()

                    if (mainViewModel.isComingFromNotification.value){
                        (activity as MainActivity).navigateToYearView()
                        mainViewModel.setIsComingFromNotification(isComing = false)
                    }else{
                        findNavController().popBackStack(R.id.viewEventFragment, inclusive = true)// Pop back two fragments by specifying the fragment ID you want to retain
                    }
                }
            }
        }

        /** Edit Event  */
        (activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.includedViewEvent.includedSave.root.apply {

            if (arguments?.containsKey(KEY_EVENT_JSON) == true){
                if (argEvent.sourceType != SourceType.CURSOR && argEvent.sourceType != SourceType.LOCAL){
                    hideViewWithAnimation(this)
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

        // Set the "All Day" status
        binding.switchAllDay.apply {
            visibility = if (event.sourceType == SourceType.REMOTE) View.INVISIBLE else View.VISIBLE
            isChecked = event.isAllDay
        }

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
        val minutesTime = if (event.alertOffset == AlertOffset.BEFORE_CUSTOM_TIME)
            event.customAlertOffset.let {value: Long? ->
                if (value == null) getString(R.string.custom_time_is_not_set)
                else {
                    "${DateUtil.timestampToMinutes(milliseconds = value)} " + resources.getString(R.string.minutes_before)
                }
            }
        else AlertOffsetConverter.toDisplayString(
            context = requireContext(),
            alertOffset = event.alertOffset
        )
        binding.tvAlertPicker.text = minutesTime
    }

    private fun navigateToNewEventFragForEdit(event: Event) {
        lifecycleScope.launch {
            // Make sure the navigation happens on the main thread
            val eventJson = GsonUtil.toJson(event)
            val bundle = Bundle().apply {
                putString(KEY_EVENT_JSON, eventJson)// Pass the event object
            }
            findNavController().navigate(R.id.newEventFragment, bundle, navOptions)
        }
    }

    override fun onDestroy() {
        lifecycleScope.coroutineContext.cancelChildren()
        //region Todo: for reset menu
        DisplayUtil.showViewWithAnimation((activity as MainActivity).binding.appBarMain.includedAppBarMainCustomToolbar.llToolbarMenu)
        //endregion
        super.onDestroy()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}