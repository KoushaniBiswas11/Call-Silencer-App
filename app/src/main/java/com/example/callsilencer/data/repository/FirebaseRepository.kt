package com.example.callsilencer.data.repository

import com.example.callsilencer.data.model.SilencedCall
import com.example.callsilencer.data.model.contact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.example.callsilencer.data.model.ScheduleSlot
import com.example.callsilencer.data.model.SpamReport

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Current user UID — null if not logged in
    private val uid get() = auth.currentUser?.uid

    // ── Allowed Contacts ──────────────────────────────────────────────────────

    suspend fun saveAllowedContacts(contacts: List<contact>) {
        val uid = uid ?: return
        val data = contacts.map {
            hashMapOf(
                "id" to it.id,
                "name" to (it.name ?: ""),
                "phoneNumber" to (it.phoneNumber ?: "")
            )
        }
        db.collection("users")
            .document(uid)
            .collection("data")
            .document("allowedContacts")
            .set(hashMapOf("contacts" to data), SetOptions.merge())
            .await()
    }

    suspend fun fetchAllowedContacts(): List<contact> {
        val uid = uid ?: return emptyList()
        return try {
            val doc = db.collection("users")
                .document(uid)
                .collection("data")
                .document("allowedContacts")
                .get()
                .await()

            val raw = doc.get("contacts") as? List<Map<String, String>>
                ?: return emptyList()

            raw.map {
                contact(
                    id = it["id"] ?: "",
                    name = it["name"],
                    phoneNumber = it["phoneNumber"]
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Silenced Call Logs ────────────────────────────────────────────────────

    suspend fun saveSilencedCalls(calls: List<SilencedCall>) {
        val uid = uid ?: return
        val data = calls.map {
            hashMapOf(
                "phoneNumber" to (it.phoneNumber ?: ""),
                "timestamp" to (it.timestamp ?: ""),
                "reason" to (it.reason ?: "")
            )
        }
        db.collection("users")
            .document(uid)
            .collection("data")
            .document("silencedCalls")
            .set(hashMapOf("calls" to data), SetOptions.merge())
            .await()
    }

    suspend fun fetchSilencedCalls(): List<SilencedCall> {
        val uid = uid ?: return emptyList()
        return try {
            val doc = db.collection("users")
                .document(uid)
                .collection("data")
                .document("silencedCalls")
                .get()
                .await()

            val raw = doc.get("calls") as? List<Map<String, String>>
                ?: return emptyList()

            raw.map {
                SilencedCall(
                    phoneNumber = it["phoneNumber"],
                    timestamp = it["timestamp"],
                    reason = it["reason"]
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Silencer Settings ─────────────────────────────────────────────────────

    suspend fun saveSettings(silencerActive: Boolean, scheduleEnabled: Boolean) {
        val uid = uid ?: return
        db.collection("users")
            .document(uid)
            .collection("data")
            .document("settings")
            .set(
                hashMapOf(
                    "silencerActive" to silencerActive,
                    "scheduleEnabled" to scheduleEnabled
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun fetchSettings(): Map<String, Boolean> {
        val uid = uid ?: return emptyMap()
        return try {
            val doc = db.collection("users")
                .document(uid)
                .collection("data")
                .document("settings")
                .get()
                .await()

            mapOf(
                "silencerActive" to (doc.getBoolean("silencerActive") ?: false),
                "scheduleEnabled" to (doc.getBoolean("scheduleEnabled") ?: false)
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
    // ── Schedule Slots ────────────────────────────────────────────────────────

    suspend fun saveScheduleSlots(slots: List<ScheduleSlot>) {
        val uid = uid ?: return
        val data = slots.map {
            hashMapOf(
                "id" to it.id,
                "label" to it.label,
                "startHour" to it.startHour,
                "startMinute" to it.startMinute,
                "endHour" to it.endHour,
                "endMinute" to it.endMinute,
                "isEnabled" to it.isEnabled
            )
        }
        db.collection("users")
            .document(uid)
            .collection("data")
            .document("scheduleSlots")
            .set(hashMapOf("slots" to data), SetOptions.merge())
            .await()
    }

    suspend fun fetchScheduleSlots(): List<ScheduleSlot> {
        val uid = uid ?: return emptyList()
        return try {
            val doc = db.collection("users")
                .document(uid)
                .collection("data")
                .document("scheduleSlots")
                .get()
                .await()

            val raw = doc.get("slots") as? List<Map<String, Any>> ?: return emptyList()
            raw.map {
                ScheduleSlot(
                    id = it["id"] as? String ?: "",
                    label = it["label"] as? String ?: "",
                    startHour = (it["startHour"] as? Long)?.toInt() ?: 0,
                    startMinute = (it["startMinute"] as? Long)?.toInt() ?: 0,
                    endHour = (it["endHour"] as? Long)?.toInt() ?: 0,
                    endMinute = (it["endMinute"] as? Long)?.toInt() ?: 0,
                    isEnabled = it["isEnabled"] as? Boolean ?: true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

// ── Community Spam Database ───────────────────────────────────────────────

    suspend fun reportSpamNumber(phoneNumber: String, reason: String) {
        val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")
        val docRef = db.collection("spam_database").document(cleaned)

        db.runTransaction { transaction ->
            val doc = transaction.get(docRef)
            val currentCount = doc.getLong("reportCount") ?: 0
            transaction.set(
                docRef,
                hashMapOf(
                    "phoneNumber" to cleaned,
                    "reportCount" to currentCount + 1,
                    "reason" to reason,
                    "lastReported" to java.text.SimpleDateFormat(
                        "dd MMM yyyy", java.util.Locale.getDefault()
                    ).format(java.util.Date())
                ),
                SetOptions.merge()
            )
        }.await()
    }

    suspend fun checkSpamDatabase(phoneNumber: String): SpamReport? {
        val cleaned = phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10)
        return try {
            val doc = db.collection("spam_database")
                .document(cleaned)
                .get()
                .await()

            if (doc.exists()) {
                SpamReport(
                    phoneNumber = doc.getString("phoneNumber") ?: cleaned,
                    reportCount = (doc.getLong("reportCount") ?: 0).toInt(),
                    reason = doc.getString("reason") ?: "Spam",
                    lastReported = doc.getString("lastReported") ?: ""
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkNumLookupApi(phoneNumber: String): Boolean {
        return try {
            val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
            val url = java.net.URL("https://api.numlookupapi.com/v1/info/$cleaned")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000

            val response = connection.inputStream.bufferedReader().readText()
            // Check if response contains spam indicators
            response.contains("\"spam\":true") ||
                    response.contains("\"fraud_score\":9") ||
                    response.contains("\"fraud_score\":10")
        } catch (e: Exception) {
            false
        }
    }

    fun isLoggedIn() = auth.currentUser != null
}