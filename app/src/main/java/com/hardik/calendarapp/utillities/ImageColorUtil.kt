package com.hardik.calendarapp.utillities

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.palette.graphics.Palette
import com.hardik.calendarapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageColorUtil {

     val monthImgResource = intArrayOf(
        R.drawable.bkg_01_jan,
        R.drawable.bkg_02_feb,
        R.drawable.bkg_03_mar,
        R.drawable.bkg_04_apr,
        R.drawable.bkg_05_may,
        R.drawable.bkg_06_jun,
        R.drawable.bkg_07_jul,
        R.drawable.bkg_08_aug,
        R.drawable.bkg_09_sep,
        R.drawable.bkg_10_oct,
        R.drawable.bkg_11_nov,
        R.drawable.bkg_12_dec
    )

    /**
     * Generates a list of text colors based on the image resources.
     */
    suspend fun generateColorList(context: Context): List<Int> = withContext(Dispatchers.Default) {
        val colorList = mutableListOf<Int>()

        monthImgResource.forEach { drawableResId ->
            // Get the drawable as a Bitmap
            val drawable = context.getDrawable(drawableResId) as BitmapDrawable
            val bitmap = drawable.bitmap

            // Generate a Palette from the Bitmap
            val palette = Palette.from(bitmap).generate()
            val vibrantColor = palette.vibrantSwatch?.rgb
            val mutedColor = palette.mutedSwatch?.rgb

            // Fallback to default color if no suitable color is found
            val textColor = vibrantColor ?: mutedColor ?: 0xFF000000.toInt()
            colorList.add(textColor)
        }

        return@withContext colorList
    }

    /**
     * Sets the text color for a TextView based on the ImageView's image.
     */
    suspend fun setTextColorBasedOnImage(imageView: ImageView, textView: TextView) = withContext(Dispatchers.Default) {
        // Ensure the ImageView has a drawable
        val drawable = imageView.drawable as? BitmapDrawable ?: return@withContext

        // Convert the drawable to a Bitmap
        val bitmap = drawable.bitmap

        // Generate a Palette from the Bitmap
        val palette = Palette.from(bitmap).generate()
        val vibrantColor = palette.vibrantSwatch?.rgb
        val mutedColor = palette.mutedSwatch?.rgb

        // Fallback to default color if no suitable color is found
        val textColor = vibrantColor ?: mutedColor ?: 0xFF000000.toInt()

        // Update the UI on the main thread
        withContext(Dispatchers.Main) {
            textView.setTextColor(textColor)
        }
    }

    suspend fun darkenColor(color: Int, factor: Float): Int {
        val alpha = Color.alpha(color)
        val red = (Color.red(color) * factor).toInt().coerceAtMost(255)
        val green = (Color.green(color) * factor).toInt().coerceAtMost(255)
        val blue = (Color.blue(color) * factor).toInt().coerceAtMost(255)
        return Color.argb(alpha, red, green, blue)
    }
}
