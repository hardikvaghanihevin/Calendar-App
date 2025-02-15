package com.hardik.calendarapp.data.repository

import android.content.ContentResolver
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.hardik.calendarapp.common.Constants
import com.hardik.calendarapp.domain.repository.CalendarEventListener
import com.hardik.calendarapp.domain.repository.CalendarRepository
import com.hardik.calendarapp.utillities.CalendarContentObserver
import javax.inject.Inject

class CalendarRepositoryImpl @Inject constructor(context: Context) : CalendarRepository {
    private val TAG = Constants.BASE_TAG + CalendarRepositoryImpl::class.simpleName

    private val contentResolver: ContentResolver = context.contentResolver
    private val handler = Handler(Looper.getMainLooper())

    private var eventListener: CalendarEventListener? = null

    private val calendarObserver = CalendarContentObserver(handler) {
        //Log.d(TAG, "CalendarObserver - Calendar events changed!")
        eventListener?.onCalendarEventsChanged() // Listener ko notify karein
    }

    override fun registerContentObserver() {
        contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI,
            true,
            calendarObserver
        )
    }

    override fun unregisterContentObserver() {
        contentResolver.unregisterContentObserver(calendarObserver)
    }

    override fun setListener(listener: CalendarEventListener) {
        this.eventListener = listener
    }
}
