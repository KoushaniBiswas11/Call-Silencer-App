package com.example.callsilencer.data.model

data class SilencedCall(
    val phoneNumber: String?,
    val timestamp: String?,
    val reason: String? = "Unknown"
)