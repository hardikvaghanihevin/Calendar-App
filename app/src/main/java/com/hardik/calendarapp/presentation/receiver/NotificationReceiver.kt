package com.hardik.calendarapp.presentation.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.common.Constants.KEY_EVENT_JSON
import com.hardik.calendarapp.data.database.entity.AlertOffset
import com.hardik.calendarapp.data.database.entity.Event
import com.hardik.calendarapp.data.database.entity.RepeatOption
import com.hardik.calendarapp.domain.repository.EventRepository
import com.hardik.calendarapp.presentation.ui.MainActivity
import com.hardik.calendarapp.utillities.AlarmScheduler
import com.hardik.calendarapp.utillities.DateUtil
import com.hardik.calendarapp.utillities.GsonUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {
    private val TAG = BASE_TAG + NotificationReceiver::class.simpleName

    @Inject
    lateinit var eventRepository: EventRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {

            val eventJson = intent.getStringExtra(KEY_EVENT_JSON)
            val event: Event? = eventJson?.let { GsonUtil.fromJson(it, Event::class.java) }

            if (event != null) {
                CoroutineScope(Dispatchers.Default).launch {

                    scheduleRepeatingNotification(context , event)

                    showNotification(context, event, eventJson)
                }
            }
        }
    }


    private fun showNotification(context: Context, event: Event, eventJson: String) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Create the notification channel for devices with API level 26 and above
        val channelId = "event_channel_id"
        val channelName = "Event Notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(channelId, channelName, importance).apply { setDescription("Notifications for scheduled events") }
        } else {
            TODO("VERSION.SDK_INT < O")
            // For devices with API level < 26, no need for a notification channel
            null
        }

        // Intent to open MainActivity with the event data
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK// or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(KEY_EVENT_JSON, eventJson)
        }

        val stackBuilder = TaskStackBuilder.create(context).apply {
            addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
            addNextIntent(intent)
        }

        val pendingIntent = stackBuilder.getPendingIntent(
            event.id.hashCode(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            event.id.hashCode(), // Unique request code
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE//FLAG_UPDATE_CURRENT , FLAG_CANCEL_CURRENT
//        )

        // If the channel is not null, create the channel (only on devices with API level 26 and above)
        channel?.let { notificationManager.createNotificationChannel(it) }

        //val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // Build notification with default
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification_app_logo) // Fallback for small icon
            .setContentTitle(event.title)
            .setContentText(event.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            //.setSound(soundUri)  // Add notification sound
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Enable sound, vibration, and lights
            .build()

        // Show the notification
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        notificationManager.notify(event.id.hashCode(), notification)
    }

    private suspend fun scheduleRepeatingNotification(context: Context, event: Event) {
        if (event.repeatOption != RepeatOption.NEVER && event.alertOffset != AlertOffset.NONE) { // Check if repeat option is not NEVER

            val nextTriggerTime: Long

            val calculatedTriggerTime = DateUtil.calculateNextOccurrence(event.triggerTime, event.repeatOption)

            nextTriggerTime = calculatedTriggerTime //- minus
                ?: event.triggerTime //- minus

            val updatedEvent = event.copy(triggerTime = nextTriggerTime)
            AlarmScheduler.updateAlarm(context, updatedEvent, isComingFromNotificationReceiver = true)

            // Launch a coroutine to call the suspend function
            CoroutineScope(Dispatchers.IO).launch {
                eventRepository.upsertEvent(updatedEvent)
            }

        }else {
            //Never scheduling repeat for event: $event
        }
    }
}
