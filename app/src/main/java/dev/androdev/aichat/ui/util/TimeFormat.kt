package dev.androdev.aichat.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - timestamp).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "now"
        diff < 3_600_000L -> "${diff / 60_000L}m"
        diff < 86_400_000L -> "${diff / 3_600_000L}h"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L}d"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
