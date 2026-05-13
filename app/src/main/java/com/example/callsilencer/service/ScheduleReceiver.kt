package com.example.callsilencer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.callsilencer.MainActivity
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.widget.SilencerWidget

class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = CallSilencerRepository(context)
        val dndManager = DndManager(context)
        val action = intent.action
        val slotLabel = intent.getStringExtra("slotLabel") ?: "Schedule"

        when (action) {
            ACTION_START_SCHEDULE -> {
                repository.setSilencerActive(true)
                dndManager.setDnd(true)
                showNotification(context, true, slotLabel)
                SilencerWidget.updateAllWidgets(context)
            }
            ACTION_STOP_SCHEDULE -> {
                // Only stop if no other slot is currently active
                val scheduleManager = ScheduleManager(context)
                if (!scheduleManager.isAnySlotCurrentlyActive()) {
                    repository.setSilencerActive(false)
                    dndManager.setDnd(false)
                    cancelNotification(context)
                    SilencerWidget.updateAllWidgets(context)
                }
                showNotification(context, false, slotLabel)
            }
        }
    }

    private fun showNotification(context: Context, isActive: Boolean, label: String) {
        val channelId = "schedule_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Schedule Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (!isActive) return

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("📵 $label Active")
            .setContentText("Silencing calls — allowed contacts still ring")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_START_SCHEDULE = "com.example.callsilencer.START_SCHEDULE"
        const val ACTION_STOP_SCHEDULE = "com.example.callsilencer.STOP_SCHEDULE"
        const val NOTIFICATION_ID = 1003
    }
}