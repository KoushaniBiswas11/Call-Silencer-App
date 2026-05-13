package com.example.callsilencer.data.model

data class ScheduleSlot(
    val id: String,
    val label: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isEnabled: Boolean = true
)