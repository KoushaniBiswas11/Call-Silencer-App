package com.example.callsilencer.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.callsilencer.data.model.SilencedCall
import com.example.callsilencer.data.model.contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.example.callsilencer.data.model.ScheduleSlot
import com.example.callsilencer.data.model.SpamReport
import java.util.*

class CallSilencerRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("call_silencer_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val firebaseRepo = FirebaseRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val KEY_SILENCER_ACTIVE = "silencer_active"
        private const val KEY_ALLOWED_CONTACTS = "allowed_contacts_v2"
        private const val KEY_SILENCED_CALLS = "silenced_calls"
    }

    // ── Silencer State ────────────────────────────────────────────────────────

    fun isSilencerActive(): Boolean =
        prefs.getBoolean(KEY_SILENCER_ACTIVE, false)

    fun setSilencerActive(active: Boolean) {
        // Write local immediately
        prefs.edit().putBoolean(KEY_SILENCER_ACTIVE, active).apply()
        // Sync to cloud in background
        scope.launch {
            try {
                firebaseRepo.saveSettings(active, isScheduleEnabled())
            } catch (_: Exception) {}
        }
    }

    // ── Allowed Contacts ──────────────────────────────────────────────────────

    fun getAllowedContacts(): List<contact> {
        val json = prefs.getString(KEY_ALLOWED_CONTACTS, null) ?: return emptyList()
        val type = object : TypeToken<List<contact>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun updateAllowedContacts(contacts: List<contact>) {
        // Write local immediately
        prefs.edit().putString(KEY_ALLOWED_CONTACTS, gson.toJson(contacts)).apply()
        // Sync to cloud in background
        scope.launch {
            try {
                firebaseRepo.saveAllowedContacts(contacts)
            } catch (_: Exception) {}
        }
    }

    fun getAllowedContactNumbers(): Set<String> {
        return getAllowedContacts().mapNotNull { it.phoneNumber }.toSet()
    }

    fun addAllowedContactNumber(number: String) {
        val current = getAllowedContacts().toMutableList()
        val cleaned = number.trim()
        if (current.none { it.phoneNumber == cleaned }) {
            current.add(contact(id = cleaned, name = cleaned, phoneNumber = cleaned))
            updateAllowedContacts(current)
        }
    }

    fun removeAllowedContactNumber(number: String) {
        val current = getAllowedContacts().toMutableList()
        current.removeAll { it.phoneNumber == number }
        updateAllowedContacts(current)
    }

    fun isAllowedContact(incomingNumber: String): Boolean {
        val cleanedIncoming = incomingNumber.replace(Regex("[^0-9]"), "")
        return getAllowedContacts().any { contact ->
            val cleanedStored = contact.phoneNumber
                ?.replace(Regex("[^0-9]"), "") ?: ""
            cleanedIncoming.takeLast(10) == cleanedStored.takeLast(10)
        }
    }

    // ── Silenced Call Logs ────────────────────────────────────────────────────

    fun getRecentSilencedCalls(): List<SilencedCall> {
        val json = prefs.getString(KEY_SILENCED_CALLS, null) ?: return emptyList()
        val type = object : TypeToken<List<SilencedCall>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addToRecentSilenced(phoneNumber: String) {
        val current = getRecentSilencedCalls().toMutableList()
        val timestamp = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a", Locale.getDefault()
        ).format(Date())

        current.add(
            0, SilencedCall(
                phoneNumber = phoneNumber.ifBlank { "Unknown" },
                timestamp = timestamp,
                reason = "Silenced by app"
            )
        )

        val trimmed = if (current.size > 50) current.take(50) else current

        // Write local immediately
        prefs.edit().putString(KEY_SILENCED_CALLS, gson.toJson(trimmed)).apply()

        // Sync to cloud in background
        scope.launch {
            try {
                firebaseRepo.saveSilencedCalls(trimmed)
            } catch (_: Exception) {}
        }
    }

    fun clearSilencedCalls() {
        prefs.edit().remove(KEY_SILENCED_CALLS).apply()
        scope.launch {
            try {
                firebaseRepo.saveSilencedCalls(emptyList())
            } catch (_: Exception) {}
        }
    }

    // ── Sync from Firestore → Local cache ────────────────────────────────────
    // Call this on app start or after login

    suspend fun syncFromCloud() {
        if (!firebaseRepo.isLoggedIn()) return

        try {
            // Sync allowed contacts
            val cloudContacts = firebaseRepo.fetchAllowedContacts()
            if (cloudContacts.isNotEmpty()) {
                prefs.edit()
                    .putString(KEY_ALLOWED_CONTACTS, gson.toJson(cloudContacts))
                    .apply()
            }

            // Sync silenced calls
            val cloudCalls = firebaseRepo.fetchSilencedCalls()
            if (cloudCalls.isNotEmpty()) {
                prefs.edit()
                    .putString(KEY_SILENCED_CALLS, gson.toJson(cloudCalls))
                    .apply()
            }

            // Sync settings
            val settings = firebaseRepo.fetchSettings()
            if (settings.isNotEmpty()) {
                prefs.edit()
                    .putBoolean(KEY_SILENCER_ACTIVE, settings["silencerActive"] ?: false)
                    .putBoolean("schedule_enabled", settings["scheduleEnabled"] ?: false)
                    .apply()
            }
        } catch (_: Exception) {}
    }

    // ── Timer Persistence ─────────────────────────────────────────────────────

    fun saveTimerInfo(startTimeMs: Long, durationSeconds: Long) {
        prefs.edit()
            .putLong("timer_start_ms", startTimeMs)
            .putLong("timer_duration_sec", durationSeconds)
            .apply()
    }

    fun getRemainingSeconds(): Long {
        val startMs = prefs.getLong("timer_start_ms", 0L)
        val durationSec = prefs.getLong("timer_duration_sec", 0L)
        if (startMs == 0L || durationSec == 0L) return 0L
        val elapsedSec = (System.currentTimeMillis() - startMs) / 1000
        val remaining = durationSec - elapsedSec
        return if (remaining > 0) remaining else 0L
    }

    fun clearTimerInfo() {
        prefs.edit()
            .remove("timer_start_ms")
            .remove("timer_duration_sec")
            .apply()
    }

    // ── Schedule ──────────────────────────────────────────────────────────────

    fun isScheduleEnabled(): Boolean =
        prefs.getBoolean("schedule_enabled", false)

    fun setScheduleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("schedule_enabled", enabled).apply()
        scope.launch {
            try {
                firebaseRepo.saveSettings(isSilencerActive(), enabled)
            } catch (_: Exception) {}
        }
    }
    // ── Schedule Slots ────────────────────────────────────────────────────────

    fun getScheduleSlots(): List<ScheduleSlot> {
        val json = prefs.getString("schedule_slots", null) ?: return emptyList()
        val type = object : TypeToken<List<ScheduleSlot>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveScheduleSlots(slots: List<ScheduleSlot>) {
        prefs.edit().putString("schedule_slots", gson.toJson(slots)).apply()
        // Sync to cloud
        scope.launch {
            try {
                firebaseRepo.saveScheduleSlots(slots)
            } catch (_: Exception) {}
        }
    }

    fun addScheduleSlot(slot: ScheduleSlot) {
        val current = getScheduleSlots().toMutableList()
        current.add(slot)
        saveScheduleSlots(current)
    }

    fun removeScheduleSlot(slotId: String) {
        val current = getScheduleSlots().toMutableList()
        current.removeAll { it.id == slotId }
        saveScheduleSlots(current)
    }

    fun updateScheduleSlot(slot: ScheduleSlot) {
        val current = getScheduleSlots().toMutableList()
        val index = current.indexOfFirst { it.id == slot.id }
        if (index != -1) {
            current[index] = slot
            saveScheduleSlots(current)
        }
    }

// ── Spam Reports ──────────────────────────────────────────────────────────

    fun getSpamReports(): List<SpamReport> {
        val json = prefs.getString("spam_reports", null) ?: return emptyList()
        val type = object : TypeToken<List<SpamReport>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveSpamReports(reports: List<SpamReport>) {
        prefs.edit().putString("spam_reports", gson.toJson(reports)).apply()
    }

    fun isSpamNumber(phoneNumber: String): Boolean {
        val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")
        return getSpamReports().any { report ->
            report.phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10) ==
                    cleaned.takeLast(10)
        }
    }
}