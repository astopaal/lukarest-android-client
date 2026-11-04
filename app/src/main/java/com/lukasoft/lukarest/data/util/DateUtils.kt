package com.lukasoft.lukarest.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val trLocale = Locale.forLanguageTag("tr-TR")
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM", trLocale)
    private val dayNameFormat = SimpleDateFormat("EEE", trLocale)

    data class DayInfo(
        val isoDate: String,      // "2026-11-02"
        val dayName: String,      // "Pzt"
        val displayDate: String,  // "02 Kas"
        val dayNumber: String     // "2"
    )

    fun getCurrentWeekMonday(): String {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        // In Calendar, MONDAY is day 2. Let's make sure it goes to Monday of current week.
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        return isoFormat.format(cal.time)
    }

    fun getWeekDays(weekStartDateIso: String): List<DayInfo> {
        val list = mutableListOf<DayInfo>()
        val cal = Calendar.getInstance()
        val parsed = try {
            isoFormat.parse(weekStartDateIso) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        cal.time = parsed

        for (i in 0 until 7) {
            val iso = isoFormat.format(cal.time)
            val dName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Pzt"
                Calendar.TUESDAY -> "Sal"
                Calendar.WEDNESDAY -> "Çar"
                Calendar.THURSDAY -> "Per"
                Calendar.FRIDAY -> "Cum"
                Calendar.SATURDAY -> "Cmt"
                Calendar.SUNDAY -> "Paz"
                else -> ""
            }
            val disp = displayFormat.format(cal.time)
            val dNum = cal.get(Calendar.DAY_OF_MONTH).toString()
            list.add(DayInfo(iso, dName, disp, dNum))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    fun addWeeks(weekStartDateIso: String, weeksToAdd: Int): String {
        val cal = Calendar.getInstance()
        val parsed = try {
            isoFormat.parse(weekStartDateIso) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        cal.time = parsed
        cal.add(Calendar.DAY_OF_MONTH, weeksToAdd * 7)
        return isoFormat.format(cal.time)
    }

    fun getTodayIso(): String {
        return isoFormat.format(Date())
    }

    fun getTurkeyTodayIso(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("Europe/Istanbul")
        }
        return formatter.format(Date())
    }
}

