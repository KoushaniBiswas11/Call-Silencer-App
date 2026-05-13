package com.example.callsilencer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.callsilencer.data.repository.CallSilencerRepository

@Composable
fun AllowedContactsDialog(
    repository: CallSilencerRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var allowedList by remember {
        mutableStateOf(repository.getAllowedContactNumbers().toList())
    }
    var manualNumber by remember { mutableStateOf("") }
    var showCallLog by remember { mutableStateOf(false) }
    var callLogNumbers by remember { mutableStateOf<List<String>>(emptyList()) }

    // Must be declared before use in loadCallLog
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val numbers = fetchRecentCallLogNumbers(context)
            callLogNumbers = numbers
            showCallLog = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allowed Contacts") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                if (showCallLog) {
                    Text(
                        "Recent Calls — Tap Add",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(callLogNumbers) { number ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(number, modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    repository.addAllowedContactNumber(number)
                                    allowedList = repository.getAllowedContactNumbers().toList()
                                }) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Currently Allowed",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.height(200.dp)) {
                        LazyColumn {
                            items(allowedList) { number ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(number, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        repository.removeAllowedContactNumber(number)
                                        allowedList = repository.getAllowedContactNumbers().toList()
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = manualNumber,
                    onValueChange = { manualNumber = it },
                    label = { Text("Add number manually") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        callLogNumbers = fetchRecentCallLogNumbers(context)
                        showCallLog = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    }
                }) {
                    Text("Call Log")
                }
                Button(onClick = {
                    if (manualNumber.isNotBlank()) {
                        repository.addAllowedContactNumber(manualNumber.trim())
                        allowedList = repository.getAllowedContactNumbers().toList()
                        manualNumber = ""
                    }
                }) {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// Moved OUTSIDE composable — no circular reference issues
private fun fetchRecentCallLogNumbers(context: android.content.Context): List<String> {
    val numbers = mutableSetOf<String>()
    return try {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER),
            null, null,
            "${CallLog.Calls.DATE} DESC"
        )
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 50) {
                val num = it.getString(0)
                if (!num.isNullOrBlank()) {
                    numbers.add(num)
                    count++
                }
            }
        }
        numbers.toList()
    } catch (e: Exception) {
        emptyList()
    }
}
