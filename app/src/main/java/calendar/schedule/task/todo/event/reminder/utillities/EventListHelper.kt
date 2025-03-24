package calendar.schedule.task.todo.event.reminder.utillities

import android.os.Build
import calendar.schedule.task.todo.event.reminder.data.database.entity.Event
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

class EventListHelper {

    fun getEventListHelperData(newData: List<Event>, weekStart: Int): Triple<Int, Map<String, Event>, Map<String, Pair<String, Boolean>>> {
        if (newData.isEmpty()) { return Triple(weekStart, emptyMap(), emptyMap()) }

        val firstDatesByMonthMap = mutableMapOf<String, Pair<String, Boolean>>()
        val firstEventOfEachWeek = mutableMapOf<String, Event>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dateFormatter = DateTimeFormatter.ofPattern(DateUtil.DATE_FORMAT_yyyy_MM_dd)

            // Process first event of each month
            newData.map { LocalDate.parse(it.startDate, dateFormatter) to it }
                .sortedBy { it.first }
                .groupBy { it.first.year to it.first.monthValue } // ✅ Fixed incorrect month grouping
                .forEach { (_, dates) ->
                    val firstEvent = dates.first().second
                    firstDatesByMonthMap[firstEvent.startDate] = Pair(firstEvent.id, true)
                }

            // Determine first event of each week
            val firstDayOfWeek = when (weekStart) {
                Calendar.MONDAY -> DayOfWeek.MONDAY
                Calendar.SATURDAY -> DayOfWeek.SATURDAY
                else -> DayOfWeek.SUNDAY
            }

            val groupedByYearWeek = newData.groupBy { event ->
                val localDate = LocalDate.parse(event.startDate, dateFormatter)
                val weekField = WeekFields.of(firstDayOfWeek, 1).weekOfWeekBasedYear()
                val year = localDate.year
                val week = localDate.get(weekField)
                "$year-$week" // ✅ Fixed incorrect week calculation
            }

            groupedByYearWeek.forEach { (_, events) ->
                firstEventOfEachWeek[events.first().startDate] = events.first()
            }

        } else {
            val dateFormat = SimpleDateFormat(DateUtil.DATE_FORMAT_yyyy_MM_dd, Locale.getDefault())

            // Process first event of each month
            newData.mapNotNull { event ->
                dateFormat.parse(event.startDate)?.let { it to event }
            }.sortedBy { it.first }
                .groupBy {
                    val calendar = Calendar.getInstance().apply { time = it.first }
                    "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
                }
                .forEach { (_, dates) ->
                    val firstEvent = dates.first().second
                    firstDatesByMonthMap[firstEvent.startDate] = Pair(firstEvent.id, true)
                }

            // Determine first event of each week
            val calendar = Calendar.getInstance()
            val groupedByYearWeek = newData.groupBy { event ->
                dateFormat.parse(event.startDate)?.let { date ->
                    calendar.time = date
                    val year = calendar.get(Calendar.YEAR)
                    val week = calendar.get(Calendar.WEEK_OF_YEAR)
                    "$year-$week"
                } ?: ""
            }

            groupedByYearWeek.forEach { (_, events) ->
                events.firstOrNull()?.let {
                    firstEventOfEachWeek[it.startDate] = it
                }
            }
        }

        return Triple(weekStart, firstEventOfEachWeek, firstDatesByMonthMap)
    }

}