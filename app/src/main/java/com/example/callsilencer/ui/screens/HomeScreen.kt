package com.example.callsilencer.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.service.CallSilencerService
import com.example.callsilencer.service.DndManager
import com.example.callsilencer.ui.theme.*
import com.example.callsilencer.widget.SilencerWidget
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(onNavigateToBlocked: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { CallSilencerRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val dndManager = remember { DndManager(context) }

    var isPermanentActive by remember { mutableStateOf(repository.isSilencerActive()) }
    var isTimerActive by remember { mutableStateOf(false) }
    var remainingTime by remember { mutableStateOf(0L) }
    var hours by remember { mutableStateOf("1") }
    var minutes by remember { mutableStateOf("00") }

    LaunchedEffect(Unit) {
        if (!dndManager.hasDndPermission()) dndManager.requestDndPermission()
    }

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val remaining = repository.getRemainingSeconds()
                if (remaining > 0 && repository.isSilencerActive()) {
                    remainingTime = remaining
                    isTimerActive = true
                    isPermanentActive = true
                } else if (remaining <= 0 && isTimerActive) {
                    isTimerActive = false
                    isPermanentActive = false
                    repository.setSilencerActive(false)
                    repository.clearTimerInfo()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(isTimerActive, remainingTime) {
        if (isTimerActive && remainingTime > 0) {
            delay(1000)
            remainingTime -= 1
            if (remainingTime <= 0) {
                isTimerActive = false
                isPermanentActive = false
                repository.setSilencerActive(false)
                repository.clearTimerInfo()
                stopSilencerService(context)
                dndManager.setDnd(false)
                SilencerWidget.updateAllWidgets(context)
                showNotification(context, "⏱ Timer Finished", "Silencing turned off automatically")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, Color(0xFF1A1F3A))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
        ) {
            // Title
            item {
                Text(
                    "Call Silencer",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }

            // Permanent Mode Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Permanent Mode",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Silences unknown calls until turned off",
                                fontSize = 14.sp,
                                color = Muted
                            )
                        }
                        Switch(
                            checked = isPermanentActive && !isTimerActive,
                            onCheckedChange = { value ->
                                isPermanentActive = value
                                repository.setSilencerActive(value)
                                dndManager.setDnd(value)
                                if (isTimerActive) {
                                    isTimerActive = false
                                    repository.clearTimerInfo()
                                    stopSilencerService(context)
                                }
                                showNotification(
                                    context,
                                    if (value) "🔕 Silencer ON" else "🔔 Silencer OFF",
                                    if (value) "Unknown calls + all apps silent"
                                    else "All calls + apps normal"
                                )
                                SilencerWidget.updateAllWidgets(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Border
                            )
                        )
                    }
                }
            }

            // Timer Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "Temporary Timer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hours
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Hours", fontSize = 12.sp, color = Muted)
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardDeep)
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextField(
                                        value = hours,
                                        onValueChange = {
                                            if (!isTimerActive && it.length <= 2 && it.all { c -> c.isDigit() })
                                                hours = it
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            cursorColor = Primary,
                                            disabledContainerColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            disabledTextColor = Color.White
                                        ),
                                        enabled = !isTimerActive,
                                        singleLine = true
                                    )
                                }
                            }

                            Text(
                                ":",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // Minutes
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Minutes", fontSize = 12.sp, color = Muted)
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardDeep)
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextField(
                                        value = minutes,
                                        onValueChange = {
                                            if (!isTimerActive && it.length <= 2 && it.all { c -> c.isDigit() })
                                                minutes = it
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            cursorColor = Primary,
                                            disabledContainerColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            disabledTextColor = Color.White
                                        ),
                                        enabled = !isTimerActive,
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Countdown display
                        if (isTimerActive && remainingTime > 0) {
                            val h = remainingTime / 3600
                            val m = (remainingTime % 3600) / 60
                            val s = remainingTime % 60
                            val displayTime = if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
                            Text(
                                "⏱ $displayTime remaining",
                                fontSize = 16.sp,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        // Start / Stop button
                        if (!isTimerActive) {
                            Button(
                                onClick = {
                                    val h = hours.toIntOrNull() ?: 0
                                    val m = minutes.toIntOrNull() ?: 0
                                    val totalSeconds = (h * 3600L) + (m * 60L)
                                    if (totalSeconds > 0) {
                                        remainingTime = totalSeconds
                                        isTimerActive = true
                                        isPermanentActive = true
                                        repository.setSilencerActive(true)
                                        repository.saveTimerInfo(System.currentTimeMillis(), totalSeconds)
                                        dndManager.setDnd(true)
                                        val intent = Intent(context, CallSilencerService::class.java).apply {
                                            putExtra("DURATION_SECONDS", totalSeconds)
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                        Toast.makeText(context, "Timer started", Toast.LENGTH_SHORT).show()
                                        showNotification(context, "⏱ Timer Started", "Silencing for ${h}h ${m}m")
                                        SilencerWidget.updateAllWidgets(context)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Start Timer", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = {
                                    isTimerActive = false
                                    isPermanentActive = false
                                    remainingTime = 0L
                                    repository.setSilencerActive(false)
                                    repository.clearTimerInfo()
                                    stopSilencerService(context)
                                    dndManager.setDnd(false)
                                    Toast.makeText(context, "Timer stopped", Toast.LENGTH_SHORT).show()
                                    showNotification(context, "⏱ Timer Stopped", "Silencing turned off")
                                    SilencerWidget.updateAllWidgets(context)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Danger)
                            ) {
                                Text("Stop Timer", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Manage Contacts Button — INSIDE LazyColumn
            item {
                Surface(
                    onClick = onNavigateToBlocked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Manage Allowed Contacts",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF8B94CC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun stopSilencerService(context: Context) {
    val stopIntent = Intent(context, CallSilencerService::class.java).apply {
        action = CallSilencerService.ACTION_STOP
    }
    context.startService(stopIntent)
}

private fun showNotification(context: Context, title: String, message: String) {
    val channelId = "action_channel"
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId, "Action Notifications", NotificationManager.IMPORTANCE_HIGH
        ).apply { setShowBadge(false) }
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}