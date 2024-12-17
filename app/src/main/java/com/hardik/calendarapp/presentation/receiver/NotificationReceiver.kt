package com.hardik.calendarapp.presentation.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hardik.calendarapp.common.Constants.BASE_TAG

class NotificationReceiver : BroadcastReceiver() {
    private val TAG = BASE_TAG + NotificationReceiver::class.simpleName
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val title = intent.getStringExtra("event_title") ?: "Event Reminder"
            val description = intent.getStringExtra("event_description") ?: "You have an event!"
            val eventId = intent.getLongExtra("event_id", 0L)
            Log.d(TAG, "onReceive: $eventId")

            showNotification(context, title, description, eventId)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, title: String, description: String, eventId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Build notification
        val notification = NotificationCompat.Builder(context, "event_channel_id")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventId.toInt(), notification)
    }
   /* override fun onReceive(context: Context?, intent: Intent?) {
        Toast.makeText(context, "Alarm Triggered!", Toast.LENGTH_SHORT).show()

        val title = intent?.getStringExtra("title") ?: "Event Reminder"
        val description = intent?.getStringExtra("description") ?: "You have an event!"

        if (context != null) {
            // Check permission for posting notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // If permission not granted, request it
                    ActivityCompat.requestPermissions((context as? android.app.Activity) ?: return, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                    return
                }
            }
            showNotification(context, title, description)
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, "channelId")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                ActivityCompat.requestPermissions((context as? android.app.Activity) ?: return, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                return
            }
        }
        // Show the notification using NotificationManager
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }*/
}
