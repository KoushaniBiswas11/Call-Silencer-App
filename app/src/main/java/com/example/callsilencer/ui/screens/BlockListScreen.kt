package com.example.callsilencer.ui.screens

import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.example.callsilencer.data.model.contact
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.ui.theme.*

@Composable
fun BlockedListScreen() {
    val context = LocalContext.current
    val repository = remember { CallSilencerRepository(context) }

    var allowedContacts by remember { mutableStateOf(repository.getAllowedContacts()) }
    var silencedCalls by remember { mutableStateOf(repository.getRecentSilencedCalls()) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID),
                    null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val name = it.getString(0)
                        val contactId = it.getLong(1)
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId.toString()), null
                        )
                        val phone = phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) pc.getString(0) else null
                        }
                        val newContact = contact(id = contactId.toString(), name = name, phoneNumber = phone)
                        if (allowedContacts.none { c -> c.id == newContact.id }) {
                            val updated = allowedContacts + newContact
                            repository.updateAllowedContacts(updated)
                            allowedContacts = updated
                            Toast.makeText(context, "$name added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Already added", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
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
            item {
                Text(
                    "Manage Allowed Contacts",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Allowed Contacts Card
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
                            "Allowed Contacts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "These will always ring normally",
                            fontSize = 13.sp,
                            color = Muted
                        )
                        Spacer(Modifier.height(16.dp))

                        // Add button
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Add Contact from Phone",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (allowedContacts.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            allowedContacts.forEach { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            c.name ?: "Unknown",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            c.phoneNumber ?: "",
                                            fontSize = 14.sp,
                                            color = Muted
                                        )
                                    }
                                    IconButton(onClick = {
                                        val updated = allowedContacts.filter { it.id != c.id }
                                        repository.updateAllowedContacts(updated)
                                        allowedContacts = updated
                                        Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = Danger
                                        )
                                    }
                                }
                                if (c != allowedContacts.last()) {
                                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                                }
                            }
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Text("No allowed contacts yet", color = Muted, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Silenced Calls Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RECENT SILENCED CALLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedDark,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (silencedCalls.isNotEmpty()) {
                        TextButton(onClick = {
                            repository.clearSilencedCalls()
                            silencedCalls = emptyList()
                        }) {
                            Text("Clear All", color = Color(0xFFFF8577), fontSize = 13.sp)
                        }
                    }
                }
            }

            if (silencedCalls.isEmpty()) {
                item {
                    Text("No calls silenced yet", color = Muted, fontSize = 14.sp)
                }
            } else {
                items(silencedCalls) { call ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDeep)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column {
                            val displayName = call.phoneNumber?.let {
                                getContactName(context, it)
                            } ?: "Unknown"

                            Text(
                                displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
// Show number below name if name is different
                            if (displayName != call.phoneNumber) {
                                Text(
                                    call.phoneNumber ?: "",
                                    fontSize = 12.sp,
                                    color = Muted
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                call.timestamp ?: "",
                                fontSize = 14.sp,
                                color = Color(0xFF8A8FA8)
                            )
                        }
                    }
                }
            }
        }
    }
}
private fun getContactName(context: android.content.Context, phoneNumber: String): String {
    return try {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phoneNumber)
        )
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: phoneNumber
    } catch (e: Exception) {
        phoneNumber
    }
}