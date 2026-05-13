package com.example.callsilencer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.callsilencer.MainActivity
import kotlinx.coroutines.*

class CallSilencerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var remainingSeconds: Long = 0L

    companion object {
        const val CHANNEL_ID = "call_silencer_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_STOP = "ACTION_STOP_TIMER"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val durationSeconds = intent?.getLongExtra("DURATION_SECONDS", 0L) ?: 0L
        if (durationSeconds > 0) {
            remainingSeconds = durationSeconds
            // Show notification ONCE here
            startForeground(NOTIFICATION_ID, buildNotification())
            startTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var secondsElapsed = 0

            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                secondsElapsed++

                // Only update notification every 30 seconds, not every second
                if (secondsElapsed % 30 == 0) {
                    updateNotification()
                }
            }
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, CallSilencerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val totalMinutes = remainingSeconds / 60
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        val timeText = when {
            hours > 0 -> "${hours}h ${mins}m remaining"
            mins > 0  -> "${mins}m remaining"
            else      -> "Finishing soon..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("📵 Call Silencer Active")
            .setContentText(timeText)
            .setOngoing(true)              // Sticky — can't be dismissed by swipe
            .setSilent(true)               // No sound/vibration on update
            .setPriority(NotificationCompat.PRIORITY_LOW)  // LOW = no heads-up popup
            .addAction(0, "Stop", stopPendingIntent)
            .setContentIntent(openPendingIntent)
            .setOnlyAlertOnce(true)        // Only alert on first show
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Silencer Timer",
                // IMPORTANCE_LOW = no sound, no popup, just sits in notification shade
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows timer while Call Silencer is running"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}