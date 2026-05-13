package com.example.callsilencer.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.callsilencer.data.model.ScheduleSlot
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.service.ScheduleManager
import com.example.callsilencer.ui.theme.*
import java.util.UUID

@Composable
fun ScheduleScreen() {
    val context = LocalContext.current
    val repository = remember { CallSilencerRepository(context) }
    val scheduleManager = remember { ScheduleManager(context) }

    var slots by remember { mutableStateOf(repository.getScheduleSlots()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val isAnyActive = remember(slots) {
        slots.filter { it.isEnabled }.any { scheduleManager.isCurrentlyInSlot(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Background, Color(0xFF1A1F3A))))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Schedule",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Status Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDark)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (isAnyActive) Icons.Default.Bedtime else Icons.Default.WbSunny,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = if (isAnyActive) Primary else Color(0xFF7B9FFF)
                        )
                        Column {
                            Text(
                                if (isAnyActive) "Schedule Active Now" else "Schedule Inactive",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                if (isAnyActive) "Calls are being silenced" else "Normal calling hours",
                                fontSize = 14.sp,
                                color = Muted
                            )
                        }
                    }
                }
            }

            // Empty state
            if (slots.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No schedule slots yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap + to add your first silence schedule",
                                fontSize = 14.sp,
                                color = Muted
                            )
                        }
                    }
                }
            } else {
                items(slots, key = { it.id }) { slot ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (scheduleManager.isCurrentlyInSlot(slot))
                                    Color(0xFF2D3561)
                                else Surface
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    slot.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${formatTime(slot.startHour, slot.startMinute)} → ${formatTime(slot.endHour, slot.endMinute)}",
                                    fontSize = 14.sp,
                                    color = Primary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (scheduleManager.isCurrentlyInSlot(slot)) {
                                    Text(
                                        "Active now",
                                        fontSize = 12.sp,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Switch(
                                checked = slot.isEnabled,
                                onCheckedChange = { enabled ->
                                    val updated = slots.map {
                                        if (it.id == slot.id) it.copy(isEnabled = enabled) else it
                                    }
                                    slots = updated
                                    repository.saveScheduleSlots(updated)
                                    scheduleManager.scheduleAll(updated)
                                    showScheduleNotification(
                                        context,
                                        if (enabled) "📅 ${slot.label} Enabled" else "📅 ${slot.label} Disabled",
                                        if (enabled) "Auto-silence active" else "Schedule turned off"
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Border
                                )
                            )
                            IconButton(onClick = {
                                val updated = slots.filter { it.id != slot.id }
                                slots = updated
                                repository.saveScheduleSlots(updated)
                                scheduleManager.scheduleAll(updated)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Danger)
                            }
                        }
                    }
                }
            }

            // Info card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDark)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "How it works",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(12.dp))
                        listOf(
                            "Add unlimited silence slots",
                            "Each slot runs daily automatically",
                            "Toggle individual slots on/off",
                            "Allowed contacts always ring",
                            "Works even when app is closed"
                        ).forEach { point ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Primary)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(point, fontSize = 14.sp, color = Color(0xFFB8BDCC))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSlotDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newSlot ->
                val updated = slots + newSlot
                slots = updated
                repository.saveScheduleSlots(updated)
                scheduleManager.scheduleAll(updated)
                showAddDialog = false
                showScheduleNotification(
                    context,
                    "📅 Schedule Added",
                    "${newSlot.label}: ${formatTime(newSlot.startHour, newSlot.startMinute)} – ${formatTime(newSlot.endHour, newSlot.endMinute)}"
                )
            }
        )
    }
}

@Composable
fun AddSlotDialog(onDismiss: () -> Unit, onAdd: (ScheduleSlot) -> Unit) {
    var label by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf("23") }
    var startMinute by remember { mutableStateOf("30") }
    var endHour by remember { mutableStateOf("8") }
    var endMinute by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Add Schedule Slot", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Night, Work)", color = Muted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Primary
                    )
                )
                Text("Start Time", fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startHour = it },
                        label = { Text("Hour", color = Muted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Primary
                        )
                    )
                    Text(":", fontSize = 24.sp, color = Color.White)
                    OutlinedTextField(
                        value = startMinute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startMinute = it },
                        label = { Text("Min", color = Muted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Primary
                        )
                    )
                }
                Text("End Time", fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endHour = it },
                        label = { Text("Hour", color = Muted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Primary
                        )
                    )
                    Text(":", fontSize = 24.sp, color = Color.White)
                    OutlinedTextField(
                        value = endMinute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endMinute = it },
                        label = { Text("Min", color = Muted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Primary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        ScheduleSlot(
                            id = UUID.randomUUID().toString(),
                            label = label.ifBlank { "Silence" },
                            startHour = startHour.toIntOrNull() ?: 23,
                            startMinute = startMinute.toIntOrNull() ?: 30,
                            endHour = endHour.toIntOrNull() ?: 8,
                            endMinute = endMinute.toIntOrNull() ?: 30,
                            isEnabled = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "${displayHour}:${minute.toString().padStart(2, '0')} $amPm"
}

private fun showScheduleNotification(context: Context, title: String, message: String) {
    val channelId = "action_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Action Notifications", NotificationManager.IMPORTANCE_HIGH)
            .apply { setShowBadge(false) }
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
        .setContentTitle(title).setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}