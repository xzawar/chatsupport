package com.codexce.supportchat.ui.components

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTimeTest {

    @Test fun unsetTimestampIsBlank() {
        assertEquals("", formatTime(0L))
        assertEquals("", formatTime(-5L))
    }

    @Test fun todayShowsClockTime() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 34)
        }
        // Only meaningful while "now" is still the same day as the probe, which noon guarantees.
        val text = formatTime(now.timeInMillis)
        assertTrue("expected HH:mm, got $text", Regex("""\d{2}:\d{2}""").matches(text))
        assertEquals("12:34", text)
    }

    @Test fun earlierDaysShowDayAndMonth() {
        val lastYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        val text = formatTime(lastYear.timeInMillis)
        assertTrue("expected a date, got $text", !text.contains(':') && text.isNotBlank())
    }

    @Test fun sameDayOfYearInAnotherYearIsNotToday() {
        // A naive day-of-year comparison would print a clock time for exactly one year ago.
        val oneYearAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        assertTrue(!formatTime(oneYearAgo.timeInMillis).contains(':'))
    }
}
