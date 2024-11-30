package com.hardik.calendarapp.presentation.ui.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.hardik.calendarapp.common.Constants.BASE_TAG


@SuppressLint("CustomViewStyleable")
class CustomViewYear(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {
    private val TAG = BASE_TAG + CustomViewYear::class.java.simpleName

}
