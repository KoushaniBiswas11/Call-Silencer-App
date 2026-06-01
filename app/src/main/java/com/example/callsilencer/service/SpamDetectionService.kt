package com.example.callsilencer.service

import android.content.Context
import android.util.Log
import com.example.callsilencer.data.model.SpamReport
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class SpamCheckResult(
    val isSpam: Boolean,
    val source: String = "",
    val reason: String = "",
    val confidence: String = "",
    val reportCount: Int = 0
)

class SpamDetectionService(private val context: Context) {

    private val firebaseRepo = FirebaseRepository()
    private val localRepo = CallSilencerRepository(context)

    // Hardcoded directly — no BuildConfig needed
    private val apiKey = "num_live_UMvytjTnTdYzBPwPJzHMbiEQEOsqlBpoonyjM3QQ"

    suspend fun checkNumber(phoneNumber: String): SpamCheckResult {
        return withContext(Dispatchers.IO) {

            val cleaned = phoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")

            // Validate — must be at least 7 digits
            val digitsOnly = cleaned.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length < 7) {
                return@withContext SpamCheckResult(
                    isSpam = false,
                    source = "",
                    reason = "Invalid number — too short",
                    confidence = ""
                )
            }

            // Step 1 — Local cache
            if (localRepo.isSpamNumber(cleaned)) {
                return@withContext SpamCheckResult(
                    isSpam = true,
                    source = "Community Reported",
                    reason = "Reported as spam by users",
                    confidence = "High"
                )
            }

            // Step 2 — Firestore community DB
            val communityReport = try {
                firebaseRepo.checkSpamDatabase(cleaned)
            } catch (_: Exception) { null }

            if (communityReport != null && communityReport.reportCount >= 1) {
                val reports = localRepo.getSpamReports().toMutableList()
                reports.add(
                    SpamReport(
                        phoneNumber = cleaned,
                        reportCount = communityReport.reportCount,
                        reason = communityReport.reason
                    )
                )
                localRepo.saveSpamReports(reports)
                return@withContext SpamCheckResult(
                    isSpam = true,
                    source = "Community Database",
                    reason = communityReport.reason,
                    confidence = if (communityReport.reportCount >= 10) "High" else "Medium",
                    reportCount = communityReport.reportCount
                )
            }

            // Step 3 — NumLookup API
            val apiResult = checkNumLookupApi(cleaned)
            Log.d("SPAM_CHECK", "API result for $cleaned: $apiResult")

            if (apiResult) {
                return@withContext SpamCheckResult(
                    isSpam = true,
                    source = "NumLookup API",
                    reason = "Flagged by spam database",
                    confidence = "Medium"
                )
            }

            SpamCheckResult(isSpam = false)
        }
    }

    suspend fun reportAsSpam(phoneNumber: String, reason: String) {
        withContext(Dispatchers.IO) {
            val reports = localRepo.getSpamReports().toMutableList()
            val existingIndex = reports.indexOfFirst {
                it.phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10) ==
                        phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10)
            }
            if (existingIndex != -1) {
                reports[existingIndex] = reports[existingIndex].copy(
                    reportCount = reports[existingIndex].reportCount + 1
                )
            } else {
                reports.add(SpamReport(
                    phoneNumber = phoneNumber,
                    reason = reason,
                    reportCount = 1
                ))
            }
            localRepo.saveSpamReports(reports)
            try { firebaseRepo.reportSpamNumber(phoneNumber, reason) } catch (_: Exception) {}
        }
    }

    private fun checkNumLookupApi(phoneNumber: String): Boolean {
        return try {
            val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
            val url = "https://api.numlookupapi.com/v1/info/$cleaned?apikey=$apiKey"
            Log.d("SPAM_CHECK", "Calling: $url")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            Log.d("SPAM_CHECK", "Response code: $code")

            if (code != 200) {
                connection.disconnect()
                return false
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            Log.d("SPAM_CHECK", "Response: $response")

            // Check multiple spam indicators
            response.contains("\"valid\":false") ||
                    response.contains("\"spam\":true") ||
                    response.contains("\"fraud_score\":10") ||
                    response.contains("\"fraud_score\":9") ||
                    response.contains("\"line_type\":\"fraudulent\"")

        } catch (e: Exception) {
            Log.e("SPAM_CHECK", "API Error: ${e.message}")
            false
        }
    }
}