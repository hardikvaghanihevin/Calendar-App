package com.hardik.calendarapp.presentation.ui.custom_view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.hardik.calendarapp.R

class CustomViewYear(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {

    private val yearTextView: TextView
    private val monthsGridLayout: GridLayout

    init {
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
            textSize = 20f // Customize text size
            text = "2024" // Replace dynamically as needed
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
            val monthView = CustomView(context, attributeSet).apply {
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
                weekStart(CustomView.WeekStart.MONDAY)
                monthDisplayOption(monthDisplayOption = CustomView.MonthDisplayOption.BOTH)
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


                getDateClickListener {
                    Toast.makeText(context,it,Toast.LENGTH_SHORT).show()
                }

                // Example attributes for CustomView (these depend on your implementation)
//                setRunningMonth(i) // Custom method to set month
            }
            monthsGridLayout.addView(monthView)
        }

        // Add views to the parent layout
        parentLayout.addView(yearTextView)
        parentLayout.addView(monthsGridLayout)

        // Add the parent layout to the FrameLayout
        addView(parentLayout)
    }
}
