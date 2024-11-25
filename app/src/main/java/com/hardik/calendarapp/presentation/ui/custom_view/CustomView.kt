package com.hardik.calendarapp.presentation.ui.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import com.hardik.calendarapp.R
import com.hardik.calendarapp.data.database.entity.Event
import java.text.DateFormatSymbols
import java.util.Calendar

@SuppressLint("CustomViewStyleable")
class CustomView(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {
    private final val TAG = CustomView::class.java.simpleName
    val today = Calendar.getInstance()

    //region Function for Variables todo:for programmatically useful
    // Getter and Setter for currentYear
    var currentYear: Int
        get() = _currentYear
        set(value) {
            _currentYear = value
        }

    // Getter and Setter for currentMonth
    var currentMonth: Int
        get() = _currentMonth
        set(value) {
            _currentMonth = value
            _currentMonthName = getMonthName(_currentMonth)
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

    // Increment the month
    fun incrementMonth() {
        if (currentMonth == 11) {
            _currentMonth = 0 // Reset to January
            _currentYear++     // Increment the year
        } else {
            _currentMonth++ // Move to the next month
        }
        _currentMonthName = getMonthName(currentMonth)  // Update the month name after changing the month
        postInvalidate()
    }

    // Decrement the month
    fun decrementMonth() {
        if (currentMonth == 0) {
            _currentMonth = 11 // Reset to December
            _currentYear--     // Decrement the year
        } else {
            _currentMonth-- // Move to the previous month
        }
        _currentMonthName = getMonthName(currentMonth)  // Update the month name after changing the month
        postInvalidate()
    }

    var monthNameWithYear: Boolean
        get() = _monthNameWithYear
        set(value) {
            if (_monthNameWithYear != value) { // Update only if the value changes
                _monthNameWithYear = value
                updateMonthText() // Trigger UI update
            }
        }

    fun updateMonthNameWithYear(wantToMonthNameWithYear: Boolean) {
        monthNameWithYear = wantToMonthNameWithYear
    }
    private fun updateMonthText() {
        _currentMonthName = getMonthName(currentMonth)  // Update the month name after changing the month
    }

    private fun getMonthName(month: Int): String {
        return  if (monthNameWithYear) DateFormatSymbols().months[month]+"-" + currentYear
        else DateFormatSymbols().months[month]
    }

    fun getWeekCount(year: Int, month: Int): Int { return 0 }

    fun weekStart(weekStart: WeekStart){
        this.weekStart = weekStart
    }
    fun monthDisplayOption(monthDisplayOption: MonthDisplayOption){
        this.monthDisplayOption = monthDisplayOption
    }

    fun textSizeMonth(textSizeMonth: Float){ this.textSizeMonth = textSizeMonth }
    fun textSizeDay(textSizeDay: Float){ this.textSizeDay = textSizeDay }
    fun textSizeDate(textSizeDate: Float){ this.textSizeDate = textSizeDate}
    fun textColorMonth(textColorMonth: Int?){ this.textColorMonth = textColorMonth ?: return}
    fun textColorDay(textColorDay: Int?){ this.textColorDay = textColorDay ?: return }
    fun textColorDate(textColorDate: Int?){ this.textColorDate = textColorDate ?: return }

    fun backgroundColorMonth(backgroundColorMonth: Int?){ this.backgroundColorMonth = backgroundColorMonth ?: return }
    fun backgroundColorDay(backgroundColorDay: Int?){ this.backgroundColorDay = backgroundColorDay ?: return }
    fun backgroundColorDate(backgroundColorDate: Int?){ this.backgroundColorDate = backgroundColorDate ?: return }

    fun backgroundDrawableMonth(backgroundDrawableMonth: Drawable?){ this.backgroundDrawableMonth = backgroundDrawableMonth }
    fun backgroundDrawableDay(backgroundDrawableDay: Drawable?){ this.backgroundDrawableDay = backgroundDrawableDay }
    fun backgroundDrawableDate(backgroundDrawableDate: Drawable?){ this.backgroundDrawableDate = backgroundDrawableDate }


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

    private val paint: Paint = Paint()
    private var viewWidth: Int = 10
    private var viewHeight: Int = 10
    // Convert 1dp to pixels
    val margin = (1.5 * context.resources.displayMetrics.density).toInt()
    val displayMetrics = Resources.getSystem().displayMetrics
    val screenWidth = displayMetrics.widthPixels // Total screen width in pixels
    val screenHeight = displayMetrics.heightPixels // Total screen height in pixels

//    private var lastRowPositionInMonth = IntArray(6) // Assuming 6 rows in a month view
//    private val monthBlocks = mutableListOf<Rect>()
//    private val originalMonthBlocks = mutableListOf<Rect>()
    private val daysBlocks = mutableListOf<Pair<Rect, String>>()

    private val eventsMap: MutableMap<String, List<Event>> = mutableMapOf()

    //endregion
    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CustomView)
        try {
            _currentYear = typedArray.getInt(R.styleable.CustomView_running_year, Calendar.getInstance().get(Calendar.YEAR))
            _currentMonth = typedArray.getInt(R.styleable.CustomView_running_month, Calendar.getInstance().get(Calendar.MONTH))
            _currentDate = typedArray.getInt(R.styleable.CustomView_running_date, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            Log.d(TAG, "date $currentDate")

            _monthNameWithYear = typedArray.getBoolean(R.styleable.CustomView_month_name_with_year, false)

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

            backgroundColorMonth = typedArray.getInteger(R.styleable.CustomView_background_color_month, backgroundColorMonth)
            backgroundColorDay = typedArray.getInteger(R.styleable.CustomView_background_color_day, backgroundColorDay)
            backgroundColorDate = typedArray.getInteger(R.styleable.CustomView_background_color_date, backgroundColorDate)

            backgroundDrawableMonth = typedArray.getDrawable(R.styleable.CustomView_background_drawable_month)
            backgroundDrawableDay = typedArray.getDrawable(R.styleable.CustomView_background_drawable_day)
            backgroundDrawableDate = typedArray.getDrawable(R.styleable.CustomView_background_drawable_date)

            currentMonthName = getMonthName(currentMonth)
        } finally {
            typedArray.recycle()
        }

        paint.color = Color.BLACK
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER
    }

    
    enum class WeekStart(val value: Int) {
        SUNDAY(0),
        MONDAY(1);
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Check if any date block was clicked
            for (pair in daysBlocks) {
                val rect = pair.first
                if (rect.contains(x.toInt(), y.toInt())) {
                    // A date block was clicked, trigger the listener
                    Log.e(TAG, "onTouchEvent: $pair", )
                    onDateItemClickListener?.invoke(pair.second) // pair.second is the date (String)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

/**     todo: here responsiveness to layout
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val blockWidth = screenWidth / 7f
        val monthNameHeight = paint.textSize * 2
        val dayNameHeight = paint.textSize * 1.5f
        val availableHeight = screenHeight - monthNameHeight - dayNameHeight // or (screenHeight - monthNameHeight - dayNameHeight)/6f */

        // Calculate basic parameters
        val blockWidth = viewWidth / 7f
        val monthNameHeight = viewHeight * 0.15f
        val dayNameHeight = viewHeight * 0.13f
        val availableHeight = viewHeight - monthNameHeight - dayNameHeight // or (screenHeight - monthNameHeight - dayNameHeight)/6f
        val dateBlockHeight = availableHeight / 6f

        // Draw the month name
        drawMonthName(canvas, blockWidth, monthNameHeight)
        // Draw the day names
        drawDayNames(canvas, blockWidth, monthNameHeight, dayNameHeight)
        // Draw the date blocks
        drawDateBlocks(canvas, blockWidth, monthNameHeight, dayNameHeight, availableHeight)

    }

    private fun drawMonthName(canvas: Canvas, blockWidth: Float, monthNameHeight: Float) {
        // Set paint properties for the background rectangle
        paint.color = backgroundColorMonth // Set the custom background color
        paint.style = Paint.Style.FILL

        // Define the rectangle bounds to fill the entire space allocated for the month name
        var rectLeft = 0f + margin // Start from the left edge of the view
        var rectRight = (viewWidth - margin).toFloat() // Extend to the right edge of the view
        var rectTop = 0f + margin // Start from the top of the month name area
        var rectBottom = (monthNameHeight - margin).toFloat() // Extend to the full height of the month name area

        // Draw the rectangle
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)

        //Set paint properties for background rectangle on textview
        paint.color = Color.RED // Set a custom background color
        paint.style = Paint.Style.FILL

        // Calculate rectangle bounds
       val textWidth = paint.measureText(currentMonthName)
       val textHeight = paint.textSize
       rectLeft = (viewWidth / 2f) - (textWidth / 2) - 16 // Add padding if needed
       rectRight = (viewWidth / 2f) + (textWidth / 2) + 16
       rectTop = (monthNameHeight / 2 - textHeight / 2) - 8 // Add padding if needed
       rectBottom = (monthNameHeight / 2 + textHeight / 2) + 8

        // Draw the rectangle
        /**canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint) //todo: if you give background on text view only */

        // If a background drawable is set, draw it
        backgroundDrawableMonth?.let { drawable ->
            // Set bounds for the drawable to fill the entire month name area
            modifyAndApplyDrawable(drawable,margin.toFloat(),0.0F , 0.0F, viewWidth.toFloat(),monthNameHeight, canvas, Color.WHITE)
            //drawable.setBounds((0  + margin), (0 + margin), (viewWidth - margin), (monthNameHeight - margin).toInt())
            //drawable.draw(canvas)
        }

        // Set paint properties for text
        paint.color = textColorMonth //Color.BLUE //Set the text color
        // Set text size dynamically
        textSizeMonth = if (textSizeMonth > 0) textSizeMonth else monthNameHeight * 0.5f
        paint.textSize = textSizeMonth
        paint.textAlign = Paint.Align.CENTER

        // Get the FontMetrics to calculate the baseline offset
        val fontMetrics = paint.fontMetrics
        val textHeight1 = fontMetrics.descent - fontMetrics.ascent
        val baselineOffset = (textHeight1 / 2) - fontMetrics.descent

        // Draw the text
        canvas.drawText(
            currentMonthName,
            viewWidth / 2f, //todo: Horizontally centered
            (monthNameHeight / 2) + baselineOffset, //todo: Vertically centered
//            monthNameHeight / 2 + paint.textSize / 2,
            paint
        )
    }

    private fun drawDayNames(canvas: Canvas, blockWidth: Float, monthNameHeight: Float, dayNameHeight: Float) {
        // If the block width is smaller set Char on day names
        val dayNames = if (blockWidth >= 70F) if(weekStart == WeekStart.SUNDAY) arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") else if (weekStart == WeekStart.MONDAY) arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        else throw IllegalArgumentException("Invalid week start value")
        else if(weekStart == WeekStart.SUNDAY) arrayOf("S", "M", "T", "W", "T", "F", "S") else if (weekStart == WeekStart.MONDAY) arrayOf("M", "T", "W", "T", "F", "S", "S")
        else throw IllegalArgumentException("Invalid week start value")

        for (i in dayNames.indices) {
            val left = i * blockWidth
            val top = monthNameHeight
            val right = left + blockWidth
            val bottom = top + dayNameHeight


            // Draw the background using the drawable if available
            backgroundDrawableDay?.let { drawable ->
                // Adjust the bounds to include a 1dp margin
                modifyAndApplyDrawable(drawable,margin.toFloat(),left , top, right,bottom, canvas, null)
                //drawable.setBounds((left + margin).toInt(), (top + margin).toInt(), (right - margin).toInt(), (bottom - margin).toInt())
                //drawable.draw(canvas)
            } ?: run {
                // If no drawable is set, use a solid color
                paint.color = backgroundColorDay
                canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paint)//canvas.drawRect(left, top, right, bottom, paint)
            }
            //paint.color = backgroundColorDay//
            //canvas.drawRect(left, top, right, bottom, paint)

            // Set text size dynamically
            textSizeDay = if (textSizeDay > 0) textSizeDay else monthNameHeight * 0.4f
/**         //todo:Shorted instead of using draw directly
            paint.color = textColorDay//Color.WHITE
            paint.textSize = textSizeDay//dayNameHeight * 0.5f
            canvas.drawText(
                dayNames[i],
                left + blockWidth / 2,
                top + dayNameHeight / 2 + paint.textSize / 3,
                paint
            )*/
            drawDateText(canvas,dayNames[i], textSizeDay, left, blockWidth, top, dayNameHeight, textColorDay)
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
        //Log.d(TAG, "drawDateBlocks: ")
        daysBlocks.clear()

        val calendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1)
        }

        // Set text size dynamically
        textSizeDate = if (textSizeDate > 0) textSizeDate else monthNameHeight * 0.35f
        val weekStartDay:Int =  if(weekStart == WeekStart.SUNDAY) 1 else if (weekStart == WeekStart.MONDAY) 2 else throw IllegalArgumentException("Invalid week start value")

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - weekStartDay + 7) % 7 //val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7 //Todo: Adjust -2 for Monday start Or -1 for Sunday start
        val dateBlockHeight = availableHeight / 6f

        // Previous month's details
        val prevCalendar = Calendar.getInstance().apply { set(currentYear, currentMonth - 1, 1) }
        val daysInPrevMonth = prevCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Draw previous month's dates
        var prevDayCounter = daysInPrevMonth - firstDayOfWeek + 1
        for (col in 0 until firstDayOfWeek) {
            val left = col * blockWidth
            val top = monthNameHeight + dayNameHeight
            val right = left + blockWidth
            val bottom = top + dateBlockHeight


            if (monthDisplayOption == MonthDisplayOption.PREVIOUS || monthDisplayOption == MonthDisplayOption.BOTH){
            // Draw the background using the drawable if available
            backgroundDrawableDate?.let { drawable ->
                // Adjust the bounds to include a 1dp margin
                modifyAndApplyDrawable(drawable,margin.toFloat(), left, top, right, bottom, canvas, Color.LTGRAY,)
                //drawable.setBounds((left + margin).toInt(), (top + margin).toInt(), (right - margin).toInt(), (bottom - margin).toInt())
                //drawable.draw(canvas)
            } ?: run {
                // If no drawable is set, use a solid color
                paint.color = Color.LTGRAY // Color for previous month's dates
                canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paint)//canvas.drawRect(left, top, right, bottom, paint)
            }

/**             //todo:Shorted instead of using draw directly
                paint.color = Color.GRAY
                paint.textSize = textSizeDate//dateBlockHeight * 0.5f
                canvas.drawText(
                    prevDayCounter.toString(),
                    left + blockWidth / 2,
                    top + dateBlockHeight / 2 + paint.textSize / 3,
                    paint
                )*/
                drawDateText(canvas, prevDayCounter.toString(), textSizeDate, left, blockWidth, top, dateBlockHeight, Color.GRAY)
            }

            // Add the day block to the list
            // Store the rect for the previous month's blocks
            val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
//            monthBlocks.add(rect)
//            originalMonthBlocks.add(Rect(rect)) // Copy to store for click animations
            daysBlocks.add(Pair(rect, "$currentYear-$currentMonth-$prevDayCounter")) // Store the Rect and day
            prevDayCounter++
        }

        // Draw current month's dates
        var dayCounter = 1
        for (row in 0 until 6) {
            for (col in 0 until 7) {
                val index = row * 7 + col
                if (index >= firstDayOfWeek && dayCounter <= daysInMonth) {
                    val left = col * blockWidth
                    val top = monthNameHeight + dayNameHeight + row * dateBlockHeight
                    val right = left + blockWidth
                    val bottom = top + dateBlockHeight


                    @Suppress("NAME_SHADOWING") val calendar = Calendar.getInstance()
                    // Check if this is the current day
                    //val isToday = (dayCounter == currentDate && currentMonth == today.get(Calendar.MONTH) && currentYear == today.get(Calendar.YEAR))
                    val isToday = (dayCounter == calendar.get(Calendar.DAY_OF_MONTH) && currentMonth == calendar.get(Calendar.MONTH) && currentYear == calendar.get(Calendar.YEAR))

                    // Draw the background using the drawable if available
                    backgroundDrawableDate?.let { drawable ->
                        // Adjust the bounds to include a 1dp margin
                        modifyAndApplyDrawable(drawable, margin.toFloat(), left, top, right, bottom, canvas, if (isToday) Color.YELLOW else Color.WHITE)
                        //drawable.setBounds((left + margin).toInt(), (top + margin).toInt(), (right - margin).toInt(), (bottom - margin).toInt())
                        //drawable.draw(canvas)
                    } ?: run {
                        // If no drawable is set, use a solid color
                        paint.color = if(isToday) Color.YELLOW else Color.WHITE//Color.LTGRAY
                        canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paint)//canvas.drawRect(left, top, right, bottom, paint)
                    }

/**                →//todo:↔ Shorted instead of using draw directly←
                    paint.color = textColorDate//Color.BLACK
                    paint.textSize = textSizeDate//dateBlockHeight * 0.5f
                    canvas.drawText(
                        dayCounter.toString(),
                        left + blockWidth / 2,
                        top + dateBlockHeight / 2 + paint.textSize / 3,
                        paint
                    )↓*/
/**
 todo: here to show indicator for events
                    val eventList = eventsMap["$currentYear-$currentMonth-$dayCounter"] ?: emptyList()
                    drawEventDotsRight(canvas, eventList, rightX = (right - margin * 4), topY = (top + margin * 2), (dateBlockHeight + margin))
                    drawEventDotsLeft(canvas, eventList, leftX = (left + margin * 4 ), topY = (top + margin * 2), (dateBlockHeight + margin))
                    drawEventDotsTop(canvas, eventList, leftX = (left + margin * 4 ), topY = (top + margin * 2), (dateBlockHeight + margin))
                    drawEventDotsBottom(canvas, eventList, leftX = (left + margin * 4 ), bottomY = (bottom - margin * 4), (dateBlockHeight + margin))*/

                    drawDateText(canvas, dayCounter.toString(), textSizeDate, left, blockWidth, top, dateBlockHeight, if (isToday) Color.BLACK else textColorDate)

                    // Store the day block for later click detection
                    val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
//                    monthBlocks.add(rect)
//                    originalMonthBlocks.add(Rect(rect)) // Store for animation
                    daysBlocks.add(Pair(rect, "$currentYear-$currentMonth-$dayCounter")) // Store the Rect and day

                    dayCounter++
                }
            }
        }

        // Draw next month's dates
        var nextDayCounter = 1
        for (index in firstDayOfWeek + daysInMonth until 42) {
            val row = index / 7
            val col = index % 7
            val left = col * blockWidth
            val top = monthNameHeight + dayNameHeight + row * dateBlockHeight
            val right = left + blockWidth
            val bottom = top + dateBlockHeight


            if(monthDisplayOption == MonthDisplayOption.NEXT || monthDisplayOption == MonthDisplayOption.BOTH){
            // Draw the background using the drawable if available
            backgroundDrawableDate?.let { drawable ->
                // Adjust the bounds to include a 1dp margin
                modifyAndApplyDrawable(drawable,margin.toFloat(), left, top, right, bottom, canvas, Color.LTGRAY)
                //drawable.setBounds((left + margin).toInt(), (top + margin).toInt(), (right - margin).toInt(), (bottom - margin).toInt())
                //drawable.draw(canvas)
            } ?: run {
                // If no drawable is set, use a solid color
                paint.color = Color.LTGRAY // Color for next month's dates
                canvas.drawRect(left + margin, top + margin, right - margin, bottom - margin, paint)//canvas.drawRect(left, top, right, bottom, paint)
            }

/**             //todo:Shorted instead of using draw directly
                paint.color = Color.GRAY
                paint.textSize = textSizeDate//dateBlockHeight * 0.5f
                canvas.drawText(
                    nextDayCounter.toString(),
                    left + blockWidth / 2,
                    top + dateBlockHeight / 2 + paint.textSize / 3,
                    paint
                )*/
                drawDateText(canvas, nextDayCounter.toString(), textSizeDate, left, blockWidth, top, dateBlockHeight, Color.GRAY)
                // Draw Event Indicators
//                val eventList = eventsMap["$currentYear-$currentMonth-$dayCounter"] ?: emptyList()
//                if (eventList.isNotEmpty()) {
//                    drawEventDots(canvas, eventList, right - margin * 2, top, dateBlockHeight)
//                }
            }

            // Add next month's day block to the list
            val rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
//            monthBlocks.add(rect)
//            originalMonthBlocks.add(Rect(rect)) // Store for animation
            daysBlocks.add(Pair(rect, "$currentYear-$currentMonth-$nextDayCounter")) // Store the Rect and day

            nextDayCounter++
        }
    }



    private fun modifyAndApplyDrawable(drawable: Drawable, margin: Float, left: Float, top: Float, right: Float, bottom: Float, canvas: Canvas, color: Int?) {
        // Ensure the drawable is a GradientDrawable
        if (drawable is GradientDrawable) {
            // Modify the color of the drawable
            color?.let { drawable.setColor(it)} // Set the desired color
            drawable.cornerRadius = 10f * context.resources.displayMetrics.density // Example: 10dp corner radius
            drawable.setStroke(
                (1 * context.resources.displayMetrics.density).toInt(), // Stroke width in dp
                Color.BLACK // Stroke color
            )
        }

        // Set bounds with margin adjustment
        drawable.setBounds(
            (left + margin).toInt(),
            (top + margin).toInt(),
            (right - margin).toInt(),
            (bottom - margin).toInt()
        )

        // Draw the drawable on the canvas
        drawable.draw(canvas)
    }

    private fun drawDateText(canvas: Canvas, text: String, textSize: Float, left: Float, blockWidth: Float, top: Float, dateBlockHeight: Float, color: Int = Color.BLACK) {
        paint.color = color
        paint.textSize = textSize
        canvas.drawText(
            text,
            left + blockWidth / 2,
            top + dateBlockHeight / 2 + paint.textSize / 3,
            paint
        )
    }

    private fun drawEventDotsTop(
        canvas: Canvas,
        events: List<Event>,
        leftX: Float,   // Left position for the first dot
        topY: Float,    // Top position of the block
        blockWidth: Float // Width of the block (full horizontal space)
    ) {
        val dotRadius = blockWidth * 0.05f // Adjust dot size relative to block width
        val spacing = dotRadius * 2.5f // Spacing between the dots
        val maxDots = (blockWidth / spacing).toInt() // Limit dots horizontally

        // Dynamically adjust the starting horizontal position to center the dots
        val startX = leftX + (blockWidth - maxDots * spacing) / 2 // Center horizontally in the block

        events.take(maxDots).forEachIndexed { index, event ->
            paint.color = Color.RED // event.eventColor // Use event's color
            val cx = startX + spacing * (index + 1)  // Position dots horizontally
            val cy = topY + dotRadius // Position the dots near the top
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }

    private fun drawEventDotsBottom(
        canvas: Canvas,
        events: List<Event>,
        leftX: Float,   // Left position for the first dot
        bottomY: Float, // Bottom position of the block (actual Y coordinate for the bottom)
        blockWidth: Float // Width of the block (full horizontal space)
    ) {
        val dotRadius = blockWidth * 0.05f // Adjust dot size relative to block width
        val spacing = dotRadius * 2.5f // Spacing between the dots
        val maxDots = (blockWidth / spacing).toInt() // Limit dots horizontally

        // Dynamically adjust the starting horizontal position to center the dots
        val startX = leftX + (blockWidth - maxDots * spacing) / 2 // Center horizontally in the block

        // Dynamically calculate the vertical position to place the dots at the bottom
        val startY = bottomY - dotRadius // Start the dots just above the bottom edge

        events.take(maxDots).forEachIndexed { index, event ->
            paint.color = Color.RED // event.eventColor // Use event's color
            val cx = startX + spacing * (index + 1)  // Position dots horizontally
            val cy = startY // Place the dots at the calculated vertical position (bottom)
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }



    private fun drawEventDotsLeft(
        canvas: Canvas,
        events: List<Event>,
        leftX: Float,  // Position on the left side
        topY: Float,
        blockHeight: Float
    ) {
        val dotRadius = blockHeight * 0.05f // Adjust dot size relative to block height
        val spacing = dotRadius * 2.5f
        val maxDots = (blockHeight / spacing).toInt() // Limit dots vertically

        // Dynamically adjust the starting vertical position to center the dots
        val startY = topY + (blockHeight - maxDots * spacing) / 2 // Center vertically in the block

        events.take(maxDots).forEachIndexed { index, event ->
            paint.color = Color.RED // event.eventColor // Use event's color
            val cx = leftX + dotRadius // Position dots on the left side
            var cy = topY + spacing * (index + 1) // Spaced vertically
            //cy = startY + index * spacing // Vertically spaced, adjusted for center alignment
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }

    private fun drawEventDotsRight(
        canvas: Canvas,
        events: List<Event>,
        rightX: Float,
        topY: Float,
        blockHeight: Float
    ) {
        val dotRadius = blockHeight * 0.05f // Adjust dot size relative to block height
        val spacing = dotRadius * 2.5f
        val maxDots = (blockHeight / spacing).toInt() // Limit dots vertically

        // Dynamically adjust the starting vertical position to center the dots
        val startY = topY + (blockHeight - maxDots * spacing) / 2 // Center vertically in the block

        events.take(maxDots).forEachIndexed { index, event ->
            paint.color = Color.RED//event.eventColor // Use event's color
            val cx = rightX - dotRadius // Position dots on the right side
            var cy = topY + spacing * (index + 1) // Spaced vertically
            //cy = startY + index * spacing // Vertically spaced, adjusted for center alignment
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }

    fun addEvent(date: String, event: Event) {
        val currentEvents = eventsMap[date]?.toMutableList() ?: mutableListOf()
        currentEvents.add(event)
        eventsMap[date] = currentEvents
        invalidate() // Redraw the calendar to reflect the changes
    }

    fun getEventsForDate(date: String): List<Event> {
        return eventsMap[date] ?: emptyList()
    }

    private var onDateItemClickListener : ((String) -> Unit)? = null

    fun getDateClickListener(listener: (String) -> Unit){
        onDateItemClickListener = listener
    }
}

//Utility for Converting dp to Pixels
fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}

interface OnDateTouchListener {
    fun onDateTouched(date: Int)  // Pass the touched date
}

