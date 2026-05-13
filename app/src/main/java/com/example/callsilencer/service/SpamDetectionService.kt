package com.example.callsilencer.service

import android.content.Context
import com.example.callsilencer.data.model.SpamReport
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun checkNumber(phoneNumber: String): SpamCheckResult {
        return withContext(Dispatchers.IO) {

            // Step 1 — Check local spam reports first (fastest)
            if (localRepo.isSpamNumber(phoneNumber)) {
                return@withContext SpamCheckResult(
                    isSpam = true,
                    source = "Community Reported",
                    reason = "Reported as spam by users",
                    confidence = "High"
                )
            }

            // Step 2 — Check community Firestore database
            val communityReport = try {
                firebaseRepo.checkSpamDatabase(phoneNumber)
            } catch (_: Exception) { null }

            if (communityReport != null && communityReport.reportCount >= 3) {
                val reports = localRepo.getSpamReports().toMutableList()
                reports.add(
                    SpamReport(
                        phoneNumber = phoneNumber,
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

            // Step 3 — Check NumLookup API
            val isApiSpam = try {
                checkNumLookupApi(phoneNumber)
            } catch (_: Exception) { false }

            if (isApiSpam) {
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
                reports.add(
                    SpamReport(
                        phoneNumber = phoneNumber,
                        reason = reason,
                        reportCount = 1
                    )
                )
            }
            localRepo.saveSpamReports(reports)

            try {
                firebaseRepo.reportSpamNumber(phoneNumber, reason)
            } catch (_: Exception) {}
        }
    }

    private fun checkNumLookupApi(phoneNumber: String): Boolean {
        return try {
            val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
            val url = java.net.URL("https://api.numlookupapi.com/v1/info/$cleaned")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode != 200) return false

            val response = connection.inputStream.bufferedReader().readText()
            response.contains("\"spam\":true") ||
                    response.contains("\"fraud_score\":9") ||
                    response.contains("\"fraud_score\":10")
        } catch (e: Exception) {
            false
        }
    }
}