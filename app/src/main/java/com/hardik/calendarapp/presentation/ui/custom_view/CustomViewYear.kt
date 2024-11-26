package com.hardik.calendarapp.presentation.ui.custom_view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import java.util.Calendar

class CustomViewYear(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {
    private val TAG = BASE_TAG + CustomViewYear::class.java.simpleName

    val yearTextView: TextView
    private val monthsGridLayout: GridLayout

    var currentYear: Int get() = _currentYear ; set(value) { _currentYear = value }
    private var _currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    fun incrementYear(year: Int = 1):Int{ _currentYear += year; postInvalidate(); return currentYear}
    fun decrementYear(year: Int = 1):Int{ _currentYear -= year; postInvalidate(); return currentYear}

    init {
        Log.d(TAG, "init: ")
        // Set up the parent layout as LinearLayout for weight distribution
        val parentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        // Set up the year text view
        yearTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, // Weight-based height
                0.05f // 5% of height
            )
            gravity = Gravity.CENTER
            visibility = View.GONE
            textSize = 20f // Customize text size
            text = "$currentYear" // Replace dynamically as needed
            setTextColor(Color.BLACK) // Customize text color
        }

        // Set up the grid layout for months
        monthsGridLayout = GridLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, // Weight-based height
                0.95f // 95% of height
            )
            rowCount = 6
            columnCount = 2
        }

        // Add 12 CustomView instances for each month
        for (i in 0 until 12) {
            val monthView = CustomViewMonth(context, attributeSet).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(i % 2, 1f)
                    rowSpec = GridLayout.spec(i / 2, 1f)
                    setMargins(8, 8, 8, 8) // Margin around each month block
                }

                // Set custom attributes dynamically (replace with actual values as needed)
                setBackgroundColor(Color.TRANSPARENT) // Example background
                currentMonth = i
                currentYear= this@CustomViewYear.currentYear
                weekStart(CustomViewMonth.WeekStart.MONDAY)
                monthDisplayOption(monthDisplayOption = CustomViewMonth.MonthDisplayOption.BOTH)
                updateMonthNameWithYear(wantToMonthNameWithYear = false)
                textColorMonth(Color.parseColor("#F80909"))
                textColorDay(context.resources.getColor(R.color.black,context.theme))
                textColorDate(null)
                backgroundColorMonth(null)
                backgroundColorDay(context.resources.getColor(android.R.color.holo_red_dark,context.theme))
                backgroundColorDate(context.resources.getColor(android.R.color.black,context.theme))
                backgroundDrawableMonth(ResourcesCompat.getDrawable(context.resources, R.drawable.day_background_1, context.theme))
                backgroundDrawableDay(ResourcesCompat.getDrawable(context.resources, R.drawable.day_background_1, context.theme))
                backgroundDrawableDate(ResourcesCompat.getDrawable(context.resources, R.drawable.day_background_1,context.theme))
                enableTouchEventHandling(enable = false)
                postInvalidate()

            }
            monthsGridLayout.addView(monthView)
            // Set click listener for each month view
            monthView.setOnClickListener {
                Log.i(TAG, "Month clicked: Year: $currentYear, month: $i")
                onMonthClickListener?.invoke(currentYear, i) // Pass the month index 'i'
            }
        }

        // Add views to the parent layout
        parentLayout.addView(yearTextView)
        parentLayout.addView(monthsGridLayout)

        // Add the parent layout to the FrameLayout
        addView(parentLayout)
    }

    fun updateYearAndMonths() {
        yearTextView.text = "$currentYear" // Update year text

        // Update each month's CustomView with the new year
        for (i in 0 until monthsGridLayout.childCount) {
            val monthView = monthsGridLayout.getChildAt(i) as CustomViewMonth
            //monthView.setUpMonth(i, _currentYear)
            monthView.currentYear = currentYear
            monthView.currentMonth = i
        }
    }

    private var onMonthClickListener: ((year:Int, month:Int)-> Unit)? = null
    fun setOnMonthClickListener(listener: (year:Int, month:Int) -> Unit) {
        onMonthClickListener = listener
    }
}
