package com.coder.app.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    // ThreadLocal prevents unnecessary memory allocations during recomposition/scrolling
    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("MMM dd", Locale.getDefault())
    }

    fun getRelativeTime(timeMillis: Long): String {
        if (timeMillis == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timeMillis

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hours ago"
            else -> dateFormat.get()?.format(Date(timeMillis)) ?: ""
        }
    }

    fun getSectionTitle(timeMillis: Long): String {
        if (timeMillis == 0L) return "Older"
        
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timeMillis }

        val diffDays = (now.timeInMillis - time.timeInMillis) / (1000 * 60 * 60 * 24)
        
        val isSameYear = now.get(Calendar.YEAR) == time.get(Calendar.YEAR)
        val dayOfYearNow = now.get(Calendar.DAY_OF_YEAR)
        val dayOfYearTime = time.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && dayOfYearNow == dayOfYearTime -> "Today"
            isSameYear && (dayOfYearNow - dayOfYearTime) == 1 -> "Yesterday"
            !isSameYear && diffDays <= 1L -> "Yesterday" // Cross-year edge case (e.g., Dec 31 - Jan 1)
            diffDays < 7 -> "Previous 7 Days"
            else -> "Older"
        }
    }
}
