package com.example.callsilencer.data.models

import java.util.UUID

data class BlockedEntry(
    val id: String = UUID.randomUUID().toString(),
    val value: String,           // e.g. "+15551234567" or "+1 (800) ***-****" or "+44 *"
    val type: BlockType,
    val label: String            // e.g. "Spam caller", "All UK numbers", "Premium rate"
)

enum class BlockType {
    NUMBER,      // exact number
    PREFIX,      // starts with (e.g. +1 800)
    COUNTRY,     // country code
    CATEGORY     // Private/Hidden, Unknown, etc.
}