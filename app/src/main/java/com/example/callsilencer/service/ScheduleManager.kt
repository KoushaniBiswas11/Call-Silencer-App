package com.example.callsilencer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.callsilencer.data.model.ScheduleSlot
import com.example.callsilencer.data.repository.CallSilencerRepository
import java.util.Calendar

class ScheduleManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val repository = CallSilencerRepository(context)

    fun scheduleAll(slots: List<ScheduleSlot>) {
        // Cancel all existing alarms first
        cancelAllAlarms()

        // Set new alarms for each enabled slot
        slots.filter { it.isEnabled }.forEachIndexed { index, slot ->
            setSlotAlarms(slot, index)

            // Check if currently inside this slot's window
            if (isCurrentlyInSlot(slot)) {
                val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
                    action = ScheduleReceiver.ACTION_START_SCHEDULE
                    putExtra("slotId", slot.id)
                    putExtra("slotLabel", slot.label)
                }
                context.sendBroadcast(startIntent)
            }
        }

        // If no slots active right now, make sure silencer is off
        val anySlotActive = slots.filter { it.isEnabled }.any { isCurrentlyInSlot(it) }
        if (!anySlotActive && slots.isNotEmpty()) {
            val stopIntent = Intent(context, ScheduleReceiver::class.java).apply {
                action = ScheduleReceiver.ACTION_STOP_SCHEDULE
            }
            context.sendBroadcast(stopIntent)
        }
    }

    private fun setSlotAlarms(slot: ScheduleSlot, index: Int) {
        val startRequestCode = 200 + (index * 2)
        val stopRequestCode = 201 + (index * 2)

        // Start alarm
        setAlarm(
            hour = slot.startHour,
            minute = slot.startMinute,
            action = ScheduleReceiver.ACTION_START_SCHEDULE,
            requestCode = startRequestCode,
            slotId = slot.id,
            slotLabel = slot.label
        )

        // Stop alarm
        setAlarm(
            hour = slot.endHour,
            minute = slot.endMinute,
            action = ScheduleReceiver.ACTION_STOP_SCHEDULE,
            requestCode = stopRequestCode,
            slotId = slot.id,
            slotLabel = slot.label
        )
    }

    private fun setAlarm(
        hour: Int,
        minute: Int,
        action: String,
        requestCode: Int,
        slotId: String,
        slotLabel: String
    ) {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            this.action = action
            putExtra("slotId", slotId)
            putExtra("slotLabel", slotLabel)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelAllAlarms() {
        // Cancel up to 50 slots (100 alarms — start + stop each)
        for (i in 0..100) {
            val intent = Intent(context, ScheduleReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 200 + i, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun isCurrentlyInSlot(slot: ScheduleSlot): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = slot.startHour * 60 + slot.startMinute
        val endMinutes = slot.endHour * 60 + slot.endMinute

        return if (startMinutes > endMinutes) {
            // Crosses midnight (e.g. 11:30 PM to 8:30 AM)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            // Same day (e.g. 9:00 AM to 5:00 PM)
            currentMinutes in startMinutes until endMinutes
        }
    }

    fun isAnySlotCurrentlyActive(): Boolean {
        return repository.getScheduleSlots()
            .filter { it.isEnabled }
            .any { isCurrentlyInSlot(it) }
    }

    // Keep old function for backward compatibility
    fun scheduleDaily(enabled: Boolean) {
        if (enabled) {
            val defaultSlot = ScheduleSlot(
                id = "default",
                label = "Night Silence",
                startHour = 23,
                startMinute = 30,
                endHour = 8,
                endMinute = 30,
                isEnabled = true
            )
            scheduleAll(listOf(defaultSlot))
        } else {
            cancelAllAlarms()
            val stopIntent = Intent(context, ScheduleReceiver::class.java).apply {
                action = ScheduleReceiver.ACTION_STOP_SCHEDULE
            }
            context.sendBroadcast(stopIntent)
        }
    }

    fun isCurrentlyInScheduleWindow(): Boolean = isAnySlotCurrentlyActive()
}