package com.example.callsilencer.service

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.callsilencer.data.repository.CallSilencerRepository

class DndManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasDndPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun requestDndPermission() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
        )
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun setDnd(enable: Boolean) {
        if (!hasDndPermission()) return

        if (enable) {
            // Enable DND Priority mode
            // Priority mode = alarms + allowed contacts go through
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
            updateDndPolicy()
        } else {
            // Disable DND — back to normal
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
    }

    private fun updateDndPolicy() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val policy = NotificationManager.Policy(
            // Allow these to break through DND:
            NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES or
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS,

            // Who can call through DND:
            NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS,

            // Who can message through DND:
            NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
        )
        notificationManager.setNotificationPolicy(policy)
    }

    fun isCurrentlyInDnd(): Boolean {
        return notificationManager.currentInterruptionFilter !=
                NotificationManager.INTERRUPTION_FILTER_ALL
    }
}