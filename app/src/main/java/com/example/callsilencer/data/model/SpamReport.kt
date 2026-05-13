package com.example.callsilencer.data.model

data class SpamReport(
    val phoneNumber: String,
    val reportCount: Int = 0,
    val reason: String = "Spam",
    val lastReported: String = ""
)