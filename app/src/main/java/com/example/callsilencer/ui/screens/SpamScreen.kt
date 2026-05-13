package com.example.callsilencer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
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
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.service.SpamDetectionService
import com.example.callsilencer.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SpamScreen() {
    val context = LocalContext.current
    val repository = remember { CallSilencerRepository(context) }
    val spamService = remember { SpamDetectionService(context) }
    val scope = rememberCoroutineScope()

    var spamList by remember { mutableStateOf(repository.getSpamReports()) }
    var searchNumber by remember { mutableStateOf("") }
    var reportNumber by remember { mutableStateOf("") }
    var reportReason by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf("") }

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
            item {
                Text(
                    "Spam Detection",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Check number card
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
                            "Check a Number",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "Check if a number is spam before calling back",
                            fontSize = 13.sp,
                            color = Muted
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = searchNumber,
                            onValueChange = {
                                searchNumber = it
                                checkResult = ""
                            },
                            label = { Text("Enter phone number", color = Muted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Primary
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (searchNumber.isNotBlank()) {
                                    isChecking = true
                                    checkResult = ""
                                    scope.launch {
                                        val result = spamService.checkNumber(searchNumber)
                                        checkResult = if (result.isSpam) {
                                            "⚠️ SPAM DETECTED\nSource: ${result.source}\nReason: ${result.reason}\nConfidence: ${result.confidence}" +
                                                    if (result.reportCount > 0) "\nReported ${result.reportCount} times" else ""
                                        } else {
                                            "✅ Number appears clean\nNo spam reports found"
                                        }
                                        isChecking = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            enabled = !isChecking
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (isChecking) "Checking..." else "Check Number",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (checkResult.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (checkResult.startsWith("⚠️"))
                                            Color(0xFF3D1A1A)
                                        else Color(0xFF1A3D2A)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = checkResult,
                                    color = if (checkResult.startsWith("⚠️")) Danger else Success,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Report spam card
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
                            "Report Spam Number",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "Help the community by reporting spam",
                            fontSize = 13.sp,
                            color = Muted
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = reportNumber,
                            onValueChange = { reportNumber = it },
                            label = { Text("Phone number to report", color = Muted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Primary
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            label = { Text("Reason (e.g. Telemarketer, Fraud)", color = Muted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Primary
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (reportNumber.isNotBlank()) {
                                    scope.launch {
                                        spamService.reportAsSpam(reportNumber, reportReason.ifBlank { "Spam" })
                                        spamList = repository.getSpamReports()
                                        reportNumber = ""
                                        reportReason = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Danger)
                        ) {
                            Text("Report as Spam", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Spam list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "YOUR REPORTED SPAM (${spamList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedDark,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (spamList.isNotEmpty()) {
                        TextButton(onClick = {
                            repository.saveSpamReports(emptyList())
                            spamList = emptyList()
                        }) {
                            Text("Clear All", color = Color(0xFFFF8577), fontSize = 13.sp)
                        }
                    }
                }
            }

            if (spamList.isEmpty()) {
                item {
                    Text("No spam numbers reported yet", color = Muted, fontSize = 14.sp)
                }
            } else {
                items(spamList) { spam ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Danger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    spam.phoneNumber,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    "${spam.reason} • Reported ${spam.reportCount} times",
                                    fontSize = 13.sp,
                                    color = Muted
                                )
                            }
                            IconButton(onClick = {
                                val updated = spamList.filter { it.phoneNumber != spam.phoneNumber }
                                repository.saveSpamReports(updated)
                                spamList = updated
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}