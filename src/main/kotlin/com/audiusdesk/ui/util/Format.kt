package com.audiusdesk.ui.util

fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1_000L
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

fun formatDurationSeconds(seconds: Int): String = formatDurationMs(seconds * 1_000L)

fun formatPlayCount(count: Long): String = when {
    count >= 1_000_000 -> compact(count / 1_000_000.0, "M")
    count >= 1_000 -> compact(count / 1_000.0, "K")
    else -> count.toString()
}

private fun compact(value: Double, suffix: String): String =
    (if (value >= 100) "%.0f".format(value) else "%.1f".format(value)) + suffix
