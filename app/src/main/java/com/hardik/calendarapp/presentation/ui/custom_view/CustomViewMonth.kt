package com.hardik.calendarapp.presentation.ui.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.data.database.entity.DayKey
import com.hardik.calendarapp.data.database.entity.EventValue
import com.hardik.calendarapp.data.database.entity.MonthKey
import com.hardik.calendarapp.data.database.entity.YearKey
import com.hardik.calendarapp.utillities.DateUtil.getFormattedDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
var _selectedDate: String? = null
@SuppressLint("CustomViewStyleable")
class CustomViewMonth(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {
    private final val TAG = BASE_TAG + CustomViewMonth::class.java.simpleName
    private var designMode: Int = 1

    //region Function for Variables todo:for programmatically useful

    // Getter and Setter for selectedDate
    var selectedDate: String?
        get() = _selectedDate
        set(value) {
            _selectedDate = value
            postInvalidate() // Redraw the view when updated externally
        }

    var eventDateList: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>>
        get() = _eventDateList
        set(value) {
            _eventDateList = value
            postInvalidate()
        }
    private var _eventDateList: MutableMap<YearKey, MutableMap<MonthKey, MutableMap<DayKey, EventValue>>> = mutableMapOf()

    // Getter and Setter for currentYear
    var currentYear: Int
        get() = _currentYear
        set(value) {
            _currentYear = value
            postInvalidate()
        }

    // Getter and Setter for currentMonth
    var currentMonth: Int
        get() = _currentMonth
        set(value) {
            _currentMonth = value
            _currentMonthName = getMonthName(_currentMonth)
            postInvalidate()
        }

    // Getter and Setter for currentDate
    var currentDate: Int
        get() = _currentDate
        set(value) {
            _currentDate = value
        }

    // Getter and Setter for currentMonthName
    var currentMonthName: String
        get() = _currentMonthName
        set(value) {
            _currentMonthName = value
        }

    var monthNameWithYear: Boolean
        get() = _monthNameWithYear
        set(value) {
            if (_monthNameWithYear != value) { // Update only if the value changes
                _monthNameWithYear = value
                updateMonthText() // Trigger UI update
            }
        }

    private fun updateMonthText() {
        _currentMonthName = getMonthName(currentMonth)  // Update the month name after changing the month
    }

    private fun getMonthName(month: Int): String {
        return  if (monthNameWithYear) DateFormatSymbols().months[month]+" " + currentYear
        else DateFormatSymbols().months[month]
    }

    private var _isMonthViewVisible = true
    var isMonthViewVisible: Boolean
        get() = _isMonthViewVisible
        set(value) {
            if (_isMonthViewVisible != value) {
                _isMonthViewVisible = value
                invalidate()  // Redraw the view
                requestLayout()  // Adjust layout if necessary
            }
        }

    fun weekStart(weekStart: WeekStart){
        this.weekStart = weekStart
        invalidate()
        requestLayout()
    }

    //endregion

    //region Variables
    private var _currentYear: Int = 0
    private var _currentMonth: Int = 0
    private var _currentDate: Int = 0
    private var _currentMonthName: String = ""

    private var _monthNameWithYear: Boolean = false

    private var weekStart: WeekStart = WeekStart.SUNDAY //todo: 0 for Sunday, 1 for Monday (default to Sunday)
    private var monthDisplayOption: MonthDisplayOption = MonthDisplayOption.NONE

    private var textSizeMonth: Float = 0f
    private var textSizeDay: Float = 0f
    private var textSizeDate: Float = 0f

    private var textColorMonth: Int = Color.BLUE
    private var textColorDay: Int = Color.WHITE
    private var textColorDate: Int = Color.BLACK

    private var backgroundColorMonth: Int = Color.WHITE
    private var backgroundColorDay: Int = Color.DKGRAY
    private var backgroundColorDate: Int = Color.LTGRAY

    private var backgroundDrawableMonth: Drawable? = null
    private var backgroundDrawableDay: Drawable? = null
    private var backgroundDrawableDate: Drawable? = null
    private var backgroundDrawableDateSelection: Drawable? = null
    private var backgroundDrawableDateToday: Drawable? = null

    private val paint: Paint = Paint()

    private val paintMonth = Paint()
    private val paintDay = Paint()
    private val paintDate = Paint()

    private var viewWidth: Int = 10
    private var viewHeight: Int = 10
    // Convert 1dp to pixels
    val margin = (1.5 * context.resources.displayMetrics.density).toInt()
    val displayMetrics = Resources.getSystem().displayMetrics
    val screenWidth = displayMetrics.widthPixels // Total screen width in pixels
    val screenHeight = displayMetrics.heightPixels // Total screen height in pixels

    private val monthNameBounds = RectF()
    private val daysBlocks = mutableListOf<Triple<Rect, Canvas, String>>()

    //endregion
    init    {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CustomView)
        try {
            _currentYear = typedArray.getInt(R.styleable.CustomView_running_year, Calendar.getInstance().get(Calendar.YEAR))
            _currentMonth = typedArray.getInt(R.styleable.CustomView_running_month, Calendar.getInstance().get(Calendar.MONTH))
            _currentDate = typedArray.getInt(R.styleable.CustomView_running_date, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

            _monthNameWithYear = typedArray.getBoolean(R.styleable.CustomView_month_name_with_year, false)
            _isMonthViewVisible = typedArray.getBoolean(R.styleable.CustomView_is_month_name_view_visible, true)

            val valueWeekStart = typedArray.getInt(R.styleable.CustomView_week_start, WeekStart.SUNDAY.value)
            weekStart = WeekStart.values().first{it.value == valueWeekStart} // Default to Sunday

            val valueMonthViewMode = typedArray.getInt(R.styleable.CustomView_month_display_option, MonthDisplayOption.NONE.value)
            monthDisplayOption = MonthDisplayOption.values().first { it.value == valueMonthViewMode }

            textSizeMonth = typedArray.getDimension(R.styleable.CustomView_text_size_month, 0F) // Default value if not set
            textSizeDay = typedArray.getDimension(R.styleable.CustomView_text_size_day, 0F) // Default value if not set
            textSizeDate = typedArray.getDimension(R.styleable.CustomView_text_size_date, 0F) // Default value if not set

            textColorMonth = typedArray.getInteger(R.styleable.CustomView_text_color_month, textColorMonth)
            textColorDay = typedArray.getInteger(R.styleable.CustomView_text_color_day, textColorDay)
            textColorDate = typedArray.getInteger(R.styleable.CustomView_text_color_date, textColorDate)

            val textFontMonthResId = typedArray.getResourceId(R.styleable.CustomView_text_font_month, 0)
            val textFontDayResId = typedArray.getResourceId(R.styleable.CustomView_text_font_day, 0)
            val textFontDateResId = typedArray.getResourceId(R.styleable.CustomView_text_font_date, 0)

            if (textFontMonthResId != 0) { val typefaceMonth = ResourcesCompat.getFont(context, textFontMonthResId)
                paintMonth.typeface = typefaceMonth }

            if (textFontDayResId != 0) { val typefaceDay = ResourcesCompat.getFont(context, textFontDayResId)
                paintDay.typeface = typefaceDay }

            if (textFontDateResId != 0) { val typefaceDate = ResourcesCompat.getFont(context, textFontDateResId)
                paintDate.typeface = typefaceDate }

            backgroundColorMonth = typedArray.getInteger(R.styleable.CustomView_background_color_month, backgroundColorMonth)
            backgroundColorDay = typedArray.getInteger(R.styleable.CustomView_background_color_day, backgroundColorDay)
            backgroundColorDate = typedArray.getInteger(R.styleable.CustomView_background_color_date, backgroundColorDate)

            backgroundDrawableMonth = typedArray.getDrawable(R.styleable.CustomView_background_drawable_month)
            backgroundDrawableDay = typedArray.getDrawable(R.styleable.CustomView_background_drawable_day)
            backgroundDrawableDate = typedArray.getDrawable(R.styleable.CustomView_background_drawable_date)
            backgroundDrawableDateSelection = typedArray.getDrawable(R.styleable.CustomView_background_drawable_date_selection)
            backgroundDrawableDateToday = typedArray.getDrawable(R.styleable.CustomView_background_drawable_date_today)

            currentMonthName = getMonthName(currentMonth)
        } finally {
            typedArray.recycle()
        }
    }

    enum class WeekStart(val value: Int) {
        SUNDAY(0),
        MONDAY(1),
        SATURDAY(2),
    }
    enum class MonthDisplayOption(val value: Int) {
        NONE(0),
        PREVIOUS(1),
        NEXT(2),
        BOTH(3)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Default dimensions (e.g., if wrap_content is used)
        val desiredWidth = screenWidth//LayoutParams.MATCH_PARENT//200 // Replace with logic based on your content
        val desiredHeight = screenHeight/3 // Replace with logic based on your content

        // Resolve width and height based on the measure specs
        viewWidth = resolveSize(desiredWidth, widthMeasureSpec)
        viewHeight = resolveSize(desiredHeight, heightMeasureSpec)

        // Set the measured dimensions
        setMeasuredDimension(viewWidth, viewHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
    }
    // Flag to control touch event handling
    var shouldHandleTouch = false

    // Example setter for enabling/disabling touch event handling
    fun enableTouchEventHandling(enable: Boolean) {
        shouldHandleTouch = enable
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        // Only handle the touch event if shouldHandleTouch is true
        if (!shouldHandleTouch) {
            return super.onTouchEvent(event)
        }

        val x = event.x
        val y = event.y
        //parent?.requestDisallowInterceptTouchEvent(true)
        when(event.action){

            MotionEvent.ACTION_DOWN -> {}
            MotionEvent.ACTION_MOVE -> {}
            MotionEvent.ACTION_UP -> {
                CoroutineScope(Dispatchers.Main).launch {
                    // Check if the month name was clicked
                    if (monthNameBounds.contains(x, y)) {
                        onMonthNameClickListener?.invoke("$currentYear", "$currentMonth") // Trigger the listener
                        _selectedDate = null
                        postInvalidate()
                        return@launch
                    }

                    // Check if any date block was clicked
                    // Handle the date selection
                    for (triple in daysBlocks) {
                        val rect = triple.first
                        if (rect.contains(x.toInt(), y.toInt())) {
                            // Update selected date asynchronously

                            // Get the clicked date from the Triple
                            val clickedDate = triple.third
                            // Update selected date
                            _selectedDate = if (_selectedDate == clickedDate) null else clickedDate
                            // Trigger the listener and redraw the view
                            _selectedDate = onDateClickListener?.invoke(selectedDate.takeIf { it != null } ?: "$currentYear-$currentMonth-${0}")

                            postInvalidate()
                            return@launch
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //todo: here responsiveness to layout

        // Set padding (as a percentage of the view height/width for responsiveness)
        val verticalPadding = viewHeight * 0.02f // 2% vertical padding
        val horizontalPadding = viewWidth * 0.05f // 5% horizontal padding

        // Calculate basic parameters //// Adjusted heights with padding
        val blockWidth = viewWidth / 7f //(viewWidth - 2 * horizontalPadding) / 7f
        // Adjust monthNameHeight dynamically
        val monthNameHeight = if (isMonthViewVisible) viewHeight * 0.15f else 0f
        val dayNameHeight = viewHeight * 0.13f

        val availableHeight = viewHeight - monthNameHeight - dayNameHeight -  (1.6f * verticalPadding) // or (screenHeight - monthNameHeight - dayNameHeight)/6f

        // Draw the month name
        // Conditionally draw the month name
        if (isMonthViewVisible) {
            drawMonthName(canvas, monthNameHeight)
        }

        // Draw the day names
        // Adjust the starting position for day names
        val dayNameTop = monthNameHeight + verticalPadding
        drawDayNames(canvas, blockWidth, dayNameTop, dayNameHeight)

        // Draw the date blocks
        // Adjust the starting position for date blocks
        val dateBlockTop = dayNameHeight + verticalPadding + 5
        drawDateBlocks(canvas, blockWidth, monthNameHeight, dateBlockTop, availableHeight)

    }

    private fun drawMonthName(canvas: Canvas, monthNameHeight: Float) {
        // Set paint properties for the background rectangle
        paintMonth.color = backgroundColorMonth // Set the custom background color
        paintMonth.style = Paint.Style.FILL

        // Define the rectangle bounds to fill the entire space allocated for the month name
        val rectLeft = 0f + margin // Start from the left edge of the view
        val rectRight = (viewWidth - margin).toFloat() // Extend to the right edge of the view
        val rectTop = 0f + margin // Start from the top of the month name area
        val rectBottom = (monthNameHeight - margin).toFloat() // Extend to the full height of the month name area

        // Update the month name bounds
        monthNameBounds.set(rectLeft, rectTop, rectRight, rectBottom)

        // Draw the rectangle
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paintMonth)

        // Set paint properties for text
        paintMonth.color = textColorMonth //Color.BLUE //Set the text color
        // Set font family (set directly on paint)

        // Set text size dynamically
        textSizeMonth = if (textSizeMonth > 0) textSizeMonth else monthNameHeight * 0.5f
        paintMonth.textSize = textSizeMonth
        paintMonth.textAlign = Paint.Align.CENTER

        // Get the FontMetrics to calculate the baseline offset
        val fontMetrics = paintMonth.fontMetrics
        val textHeight1 = fontMetrics.descent - fontMetrics.ascent
        val baselineOffset = (textHeight1 / 2) - fontMetrics.descent

        // Calculate the x and y positions
        val textX = viewWidth / 2f // Add margin or padding to the left //todo: Horizontally centered = 'viewWidth / 2f' or 'margin'
        val textY = (monthNameHeight / 2) + baselineOffset //todo: Vertically centered

        // Draw the text
        canvas.drawText(
            currentMonthName.uppercase(),
            textX.toFloat(), // Left-aligned
            textY, // Vertically centered
            paintMonth
        )
    }

    private fun drawDayNames(canvas: Canvas, blockWidth: Float, monthNameHeight: Float, dayNameHeight: Float) {
        // Set paint properties for the background rectangle
        paintDay.color = backgroundColorDay // Set the custom background color
        paintDay.style = Paint.Style.FILL

        // Define horizontal padding as a percentage of the view width
        val horizontalPadding = viewWidth * 0.05f // 5% of the view width

        // Adjust the available width for day blocks after applying padding
        val adjustedBlockWidth = (viewWidth - 2 * horizontalPadding) / 7f

        // If the block width is smaller, set Char on day names
        val dayNames = if ((blockWidth.takeIf { designMode.equals(1) } ?: adjustedBlockWidth) >= 70F) {
            if (weekStart == WeekStart.SUNDAY) context.resources.getStringArray(R.array.week_2)
            else if (weekStart == WeekStart.MONDAY) context.resources.getStringArray(R.array.week_1)
            else if (weekStart == WeekStart.SATURDAY) context.resources.getStringArray(R.array.week_3)
            else throw IllegalArgumentException("Invalid week start value")
        } else {
            if (weekStart == WeekStart.SUNDAY) context.resources.getStringArray(R.array.short_week_2)
            else if (weekStart == WeekStart.MONDAY) context.resources.getStringArray(R.array.short_week_1)
            else if (weekStart == WeekStart.SATURDAY) context.resources.getStringArray(R.array.short_week_3)
            else throw IllegalArgumentException("Invalid week start value")
        }

        // If a background drawable is set, draw it
        backgroundDrawableDay?.let { drawable ->
            if (designMode.equals(1)) modifyAndApplyDrawable(drawable, margin.toFloat(), left = 0.0f, top = monthNameHeight, right = viewWidth.toFloat(), bottom = (monthNameHeight + dayNameHeight), canvas, context.getColor(R.color.white))
            //if (designMode.equals(2)) modifyAndApplyDrawable(drawable,margin.toFloat(), left = horizontalPadding , top = monthNameHeight, right = (viewWidth.toFloat() - horizontalPadding), bottom = (monthNameHeight + dayNameHeight), canvas, context.getColor(R.color.blue))//todo: week(7 days) block background
        }

        for (i in dayNames.indices) {
            val left = (i * blockWidth).takeIf { designMode.equals(1) } ?: (horizontalPadding + i * adjustedBlockWidth)
            val top = (monthNameHeight).takeIf { designMode.equals(1) } ?: (monthNameHeight)
            val right = (left + blockWidth).takeIf { designMode.equals(1) } ?: (left + adjustedBlockWidth)
            val bottom = (top + dayNameHeight).takeIf { designMode.equals(1) } ?: (top + dayNameHeight)

            // Draw the background using the drawable if available
            backgroundDrawableDay?.let { drawable ->
                if (designMode.equals(1)) modifyAndApplyDrawable(drawable, margin.toFloat(), left, top, right, bottom, canvas, context.getColor(R.color.white))
            } ?: run {
                // If no drawable is set, use a solid color
                paintDay.color = backgroundColorDay
                canvas.drawRect(left + margin + 2, top + margin, right - margin - 2, bottom - margin, paintDay)
            }

            // Check if it's Sunday and change the color
            val isSunday = when (weekStart) {
                WeekStart.SUNDAY -> i == 0 // Sunday is the first day in this configuration
                WeekStart.MONDAY -> i == 6 // Sunday is the last day in this configuration
                WeekStart.SATURDAY -> i == 1 // Sunday is the second day in this configuration
                else -> false // Default case, should not happen
            }

            // Check if it's Sunday and change the color
            paintDay.color = if (isSunday) resources.getColor(R.color.error, null) else textColorDay // Set Sunday color to red or any other color

            // Set text size dynamically
            textSizeDay = if (textSizeDay > 0) textSizeDay else dayNameHeight * 0.35f
            paintDay.textSize = textSizeDay
            paintDay.textAlign = Paint.Align.CENTER

            // Draw the day name text
            drawDateText(canvas, dayNames[i], paintDay, left, blockWidth, top, dayNameHeight)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawDateBlocks(
        canvas: Canvas,
        blockWidth: Float,
        monthNameHeight: Float,
        dayNameHeight: Float,
        availableHeight: Float
    ) {
        // Define padding as percentages or fixed values
        val horizontalPadding = viewWidth * 0.05f // 5% of the view width
        val verticalPadding = availableHeight * 0.05f // 5% of the available height

        // Adjust the available width and height for date blocks after applying padding
        val adjustedBlockWidth = (viewWidth - 2 * horizontalPadding) / 7f
        val adjustedBlockHeight = (availableHeight - 2 * verticalPadding) / 6f

        daysBlocks.clear()

        val calendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1)
        }

        paintDate.color = backgroundColorDate
        paintDate.style = Paint.Style.FILL
        // Set text size dynamically
        textSizeDate = if (textSizeDate > 0) textSizeDate else dayNameHeight * 0.35f
        paintDate.textSize = textSizeDate
        paintDate.textAlign = Paint.Align.CENTER

        val weekStartDay:Int =  if(weekStart == WeekStart.SUNDAY) 1 else if (weekStart == WeekStart.MONDAY) 2 else if (weekStart == WeekStart.SATURDAY) 7 else throw IllegalArgumentException("Invalid week start value")

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - weekStartDay + 7) % 7 //val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7 //Todo: Adjust -2 for Monday start Or -1 for Sunday start
        val dateBlockHeight = (availableHeight / 6f).takeIf { designMode.equals(1) } ?: adjustedBlockHeight

        // If a background drawable is set, draw it
        backgroundDrawableDate?.let { drawable ->
            // Set bounds for the drawable to fill the entire Date area
            if (designMode.equals(2)) modifyAndApplyDrawable(drawable,margin.toFloat(),left = 0.0f , top = (monthNameHeight + dayNameHeight), right = viewWidth.toFloat(),bottom =(monthNameHeight + dayNameHeight + 6.7f * adjustedBlockHeight ), canvas, context.getColor(R.color.background_primary))//todo: dates of month block background
        }


        // Draw previous month's dates
        if (monthDisplayOption == MonthDisplayOption.PREVIOUS || monthDisplayOption == MonthDisplayOption.BOTH) {
            // Previous month's details
            val prevCalendar = Calendar.getInstance().apply { set(currentYear, currentMonth - 1, 1) }
            val daysInPrevMonth = prevCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val prevMonth = prevCalendar.get(Calendar.MONTH)

            var prevDayCounter = daysInPrevMonth - firstDayOfWeek + 1
            for (col in 0 until firstDayOfWeek) {
                val left = (col * blockWidth).takeIf { designMode.equals(1) }
                    ?: (horizontalPadding + col * adjustedBlockWidth)
                val top = (monthNameHeight + dayNameHeight).takeIf { designMode.equals(1) }
                    ?: (monthNameHeight + dayNameHeight + verticalPadding)
                val right = (left + blockWidth).takeIf { designMode.equals(1) }
                    ?: (left + adjustedBlockWidth)
                val bottom = (top + dateBlockHeight).takeIf { designMode.equals(1) }
                    ?: (top + adjustedBlockHeight)


                if (monthDisplayOption == MonthDisplayOption.PREVIOUS || monthDisplayOption == MonthDisplayOption.BOTH) {
                    // Draw the background using the drawable if available
                    paintDate.color = context.resources.getColor(R.color.background_primary, context.theme)//Color.LTGRAY // Color for previous month's dates
                    backgroundDrawableDate?.let { drawable ->
                        if (designMode.equals(1)) modifyAndApplyDrawable(drawable, margin.toFloat(), left, top, right, bottom, canvas, Color.LTGRAY,)
                    } ?: run {
                        // If no drawable is set, use a solid color
                        canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paintDate)//canvas.drawRect(left, top, right, bottom, paint)
                    }
                        paintDate.color = context.resources.getColor(R.color.text_secondary, context.theme)//Color.LTGRAY // Color for previous month's dates
                    if (designMode.equals(1))
                        drawDateText(canvas, prevDayCounter.toString(), paintDate, left, blockWidth, top, dateBlockHeight)
                    if (designMode.equals(2))
                        drawDateText(canvas, prevDayCounter.toString(), paintDate, left, adjustedBlockWidth, top, dateBlockHeight)
                }

                // Add the day block to the list
                // Store the rect for the previous month's blocks
                val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                daysBlocks.add(
                    Triple(rect, canvas, "$currentYear-$prevMonth-$prevDayCounter")
                ) // Store the Rect and day
                prevDayCounter++
            }
        }

        // Draw current month's dates
        var dayCounter = 1
        for (row in 0 until 6) {
            for (col in 0 until 7) {
                val index = row * 7 + col
                if (index >= firstDayOfWeek && dayCounter <= daysInMonth) {
                    val left = (col * blockWidth).takeIf { designMode.equals(1) } ?: (horizontalPadding + col * adjustedBlockWidth)
                    val top = (monthNameHeight + dayNameHeight + row * dateBlockHeight).takeIf { designMode.equals(1) } ?: (monthNameHeight + dayNameHeight + verticalPadding + row * adjustedBlockHeight)
                    val right = (left + blockWidth).takeIf { designMode.equals(1) } ?: (left + adjustedBlockWidth)
                    val bottom = (top + dateBlockHeight).takeIf { designMode.equals(1) } ?: (top + adjustedBlockHeight)



                    val calendarT = Calendar.getInstance()
                    // Check if this is Sunday
                    // Calculate the date for the last column in the current week
                    val lastDayOfWeekCounter = dayCounter
                    var isSunday = false
                    // Check if it is within valid range
                    if (lastDayOfWeekCounter <= daysInMonth) {
                        val calendarS = Calendar.getInstance()
                        calendarS.set(Calendar.YEAR, currentYear)
                        calendarS.set(Calendar.MONTH, currentMonth)
                        calendarS.set(Calendar.DAY_OF_MONTH, lastDayOfWeekCounter)

                        isSunday = calendarS.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        // Add your custom logic for weeks ending on Sunday
                        if (isSunday) { paintDate.color = resources.getColor(R.color.error, null) }
                    }

                    // Check if this is the current day
                    //val isToday = (dayCounter == currentDate && currentMonth == today.get(Calendar.MONTH) && currentYear == today.get(Calendar.YEAR))
                    val isToday = (dayCounter == calendarT.get(Calendar.DAY_OF_MONTH) && currentMonth == calendarT.get(Calendar.MONTH) && currentYear == calendarT.get(Calendar.YEAR))
                    val dateString = "$currentYear-$currentMonth-$dayCounter"
                    val isSelected = dateString == _selectedDate

                    // Draw the background using the drawable if available with a selected color
                    if(isSelected){
                        //Log.e(TAG, "drawDateBlocks: isSelected", )
                        val color = resources.getColor(if (isSunday) R.color.error else R.color.text_primary, null)
                        paintDate.color = resources.getColor(if (isSunday) R.color.error else R.color.text_primary, null)

                        backgroundDrawableDateSelection?.let { drawable -> modifyAndApplyDrawable(drawable, margin.toFloat(), left, top, right, bottom, canvas, color, squareSize = (textSizeDate + 1).toInt() ) } ?:
                        run {
                            // If no drawable is set, use a solid color
                            canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paintDate)}//canvas.drawRect(left, top, right, bottom, paint)
                    }
                    else if(isToday){
                        //Log.e(TAG, "drawDateBlocks: isToday", )
                        val color = resources.getColor(if (isSunday) R.color.error else R.color.accent_primary, null)
                        paintDate.color =resources.getColor(if (isSunday) R.color.error else R.color.accent_primary, null)

                        backgroundDrawableDateToday?.let { drawable -> modifyAndApplyDrawable(drawable, margin.toFloat(), left, top, right, bottom, canvas, color, squareSize = (textSizeDate + 1).toInt() ) } ?:
                        run {
                            // If no drawable is set, use a solid color
                            canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paintDate)}//canvas.drawRect(left, top, right, bottom, paint)

                    }
                    else{
                        //Log.e(TAG, "drawDateBlocks: else", )
                        val color = resources.getColor(R.color.background_primary, null)
                        paintDate.color = resources.getColor(R.color.background_primary, null)//Color.LTGRAY

                        backgroundDrawableDate?.let { drawable -> modifyAndApplyDrawable(drawable, margin.toFloat(), left, top, right, bottom, canvas, color, squareSize = (textSizeDate + 1).toInt() ) } ?: run {
                            // If no drawable is set, use a solid color
                            canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paintDate)}//canvas.drawRect(left, top, right, bottom, paint)
                    }

                    //todo: here to show indicator for events
                     if (eventDateList.containsKey(currentYear.toString())) {
                        // The key exists in the map
                        val yearMap = eventDateList[currentYear.toString()] ?: return
                        if (yearMap.containsKey(currentMonth.toString())) {

                            val monthMap = yearMap[currentMonth.toString()] ?: return
                            if (monthMap.containsKey(dayCounter.toString())) {

                                val dayMap = monthMap[dayCounter.toString()] ?: return
                                val targetDate = "$currentYear-${currentMonth}-$dayCounter"

                                if (dayMap.getFormattedDate() == targetDate) {

                                    // then key exists in the map
                                    drawEventDotsBottom(canvas = canvas, leftX = left, bottomY = bottom, blockWidth = blockWidth)

                                }
                            }
                        }
                    }

                    val color = when {
                        isToday -> resources.getColor(R.color.white, null).takeIf { !isSelected } ?: resources.getColor(R.color.background_secondary, null)
                        isSunday -> resources.getColor(R.color.error, null).takeIf { !isSelected } ?: resources.getColor(R.color.white, null)
                        isSelected -> resources.getColor(R.color.background_secondary, null)
                        else -> textColorDate // Default fallback color
                    }
                    paintDate.color = color
                    if (designMode.equals(1)) drawDateText(canvas, dayCounter.toString(), paintDate, left, blockWidth, top, dateBlockHeight )
                    if (designMode.equals(2)) drawDateText(canvas, dayCounter.toString(), paintDate, left, blockWidth, top, dateBlockHeight )

                    // Store the day block for later click detection
                    val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                    daysBlocks.add(Triple(rect, canvas, "$currentYear-$currentMonth-$dayCounter")) // Store the Rect and day

                    dayCounter++
                }
            }
        }

        // Draw next month's dates
        if (monthDisplayOption == MonthDisplayOption.NEXT || monthDisplayOption == MonthDisplayOption.BOTH) {
            val nextMonth = (currentMonth + 1) % 12
            var nextDayCounter = 1
            for (index in firstDayOfWeek + daysInMonth until 42) {
                val row = index / 7
                val col = index % 7
                val left = (col * blockWidth).takeIf { designMode.equals(1) }
                    ?: (horizontalPadding + col * adjustedBlockWidth)
                val top = (monthNameHeight + dayNameHeight + row * dateBlockHeight).takeIf {
                    designMode.equals(1)
                } ?: (monthNameHeight + dayNameHeight + verticalPadding + row * adjustedBlockHeight)
                val right = (left + blockWidth).takeIf { designMode.equals(1) }
                    ?: (left + adjustedBlockWidth)
                val bottom = (top + dateBlockHeight).takeIf { designMode.equals(1) }
                    ?: (top + adjustedBlockHeight)


                if (monthDisplayOption == MonthDisplayOption.NEXT || monthDisplayOption == MonthDisplayOption.BOTH) {
                    // Draw the background using the drawable if available
                    paintDate.color = context.resources.getColor(R.color.background_primary, context.theme)//Color.LTGRAY // Color for next month's dates
                    backgroundDrawableDate?.let { drawable ->
                        // Adjust the bounds to include a 1dp margin
                        if (designMode.equals(1)) modifyAndApplyDrawable(drawable, margin.toFloat(), left, top, right, bottom, canvas, Color.LTGRAY)
                    } ?: run {
                        // If no drawable is set, use a solid color
                        canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paintDate)//canvas.drawRect(left, top, right, bottom, paint)
                    }
                        paintDate.color = context.resources.getColor(R.color.text_secondary, context.theme)//Color.LTGRAY // Color for next month's dates
                    if (designMode.equals(1))
                        drawDateText(canvas, nextDayCounter.toString(), paintDate, left, blockWidth, top, dateBlockHeight)
                    if (designMode.equals(2))
                        drawDateText(canvas, nextDayCounter.toString(), paintDate, left, adjustedBlockWidth, top, dateBlockHeight)
                }

                // Add next month's day block to the list
                val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                daysBlocks.add(
                    Triple(rect, canvas, "$currentYear-$nextMonth-$nextDayCounter")
                ) // Store the Rect and day

                nextDayCounter++
            }
        }
    }



    @SuppressLint("ObsoleteSdkInt")
    private fun modifyAndApplyDrawable(drawable: Drawable, margin: Float, left: Float, top: Float, right: Float, bottom: Float, canvas: Canvas, color: Int?, squareSize: Int? = null) {
        // Modify the color of the drawable
        when (drawable) {
            is GradientDrawable -> { // Ensure the drawable is a GradientDrawable
                //Log.i(TAG, "modifyAndApplyDrawable: GD", )
                // Handle manual shape drawable
                color?.let { drawable.setColor(it) } // Set the desired color
                drawable.cornerRadius = 10f * context.resources.displayMetrics.density // Example: 10dp corner radius
                drawable.setStroke(
                    (1 * context.resources.displayMetrics.density).toInt(), // Stroke width in dp
                    Color.BLACK // Stroke color
                )
            }
            is VectorDrawable -> {
                //Log.i(TAG, "modifyAndApplyDrawable: VD", )
                // Handle SVG vector drawable
                color?.let { tint ->
                    drawable.setTint(tint) // Apply tint to the vector drawable
                }
            }
            else -> {
                //Log.i(TAG, "modifyAndApplyDrawable: else")
                // Unsupported drawable type: ${drawable::class.java}
            }
        }

        // Set bounds with margin adjustment
        // Calculate the square size in pixels
        val squarePx = squareSize?.let { (it * context.resources.displayMetrics.density).toInt() } ?: 0

        // Calculate center coordinates
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        // Calculate bounds to center the drawable
        val halfSquare = squarePx / context.resources.getDimension(R.dimen.dateRoundSize)
        drawable.setBounds(
            (centerX - halfSquare).toInt(),
            (centerY - halfSquare).toInt(),
            (centerX + halfSquare).toInt(),
            (centerY + halfSquare).toInt()
        )
        // Draw the drawable on the canvas
        drawable.draw(canvas)
    }

    private fun drawDateText(canvas: Canvas, text: String, paint: Paint, left: Float, blockWidth: Float, top: Float, dateBlockHeight: Float) {
        canvas.drawText(
            text,
            left + blockWidth / 2,
            top + dateBlockHeight / 2 + paint.textSize / 3,
            paint
        )
    }

    private fun drawEventDotsBottom(canvas: Canvas, leftX: Float, bottomY: Float, blockWidth: Float) {
        val dotRadius = blockWidth * 0.045f // Adjust dot size relative to block width

        // Calculate the horizontal center position of the block
        val cx = leftX + blockWidth / 2
        // Position the dot just above the bottom edge of the block
        //val cy = bottomY - dotRadius / 2
        val cy = bottomY - dotRadius / context.resources.getDimension(R.dimen.dateRoundSize) + 0.5F //-10

        // Draw the dot
        paint.color = resources.getColor(R.color.error, null) // Set the color of the dot (default: red)
        canvas.drawCircle(cx, cy, dotRadius, paint)
    }

    private var onMonthNameClickListener: ((YearKey, MonthKey) -> Unit)? = null
    fun getMonthNameClickListener(listener: (YearKey, MonthKey) -> Unit){ onMonthNameClickListener = listener }

    private var onDateClickListener: ((String) -> String)? = null
    fun getDateClickListener(listener: (String) -> String){ onDateClickListener = listener }

}
