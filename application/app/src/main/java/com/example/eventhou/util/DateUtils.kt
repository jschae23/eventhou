@file:JvmName("DateUtils")

package com.example.eventhou.util

import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

val sdf = SimpleDateFormat("yyyy-MM-dd")

/**
 * Convert Date to date ISO format.
 */
fun dateToISO(date: Date): String {
    return sdf.format(date)
}

/**
 * Convert LocalDate to date ISO format.
 */
fun dateToISO(date: LocalDate): String {
    return date.format(DateTimeFormatter.ISO_DATE)
}

/**
 * Convert Date to UTC LocalDateTime.
 */
fun dateToUtcDateTime(date: Date): LocalDateTime {
    return date.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime()
}

/**
 * Shows the LocalDate as localized string.
 */
fun dateToLocalizedShort(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
}

/**
 * Shows the date time as localized string.
 */
fun dateTimeToLocalizedShort(date: Date): String {
    val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTimeToLocalizedShort(localDate)
}

/**
 * Shows the LocalDateTime time as localized string.
 */
fun dateTimeToLocalizedShort(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
}

/**
 * Shows a date range as localized string.
 */
fun dateRangeToLocalizedShort(date1: LocalDate, date2: LocalDate): String {
    if (date1.isBefore(date2)) {
        return dateToLocalizedShort(date1) + " - " + dateToLocalizedShort(date2)
    } else {
        return dateToLocalizedShort(date1)
    }
}

/**
 * Converts a LocalDate (in UTC) to the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
 */
fun dateToEpochMilli(date: LocalDate): Long {
    return date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
//    return date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * Obtains the LocalDate (in UTC) using milliseconds from the epoch of 1970-01-01T00:00:00Z.
 */
fun epochMilliToDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
//    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}

/**
 * Returns the dates until endDate (TODO replace in Java 9 with build in function).
 */
fun datesUntil(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var d = startDate
    while (!d.isAfter(endDate)) {
        dates.add(d)
        d = d.plusDays(1)
    }
    return dates
}
