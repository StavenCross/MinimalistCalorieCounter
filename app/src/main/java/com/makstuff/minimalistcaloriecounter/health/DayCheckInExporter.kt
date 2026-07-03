package com.makstuff.minimalistcaloriecounter.health

import android.content.Context
import com.makstuff.minimalistcaloriecounter.io.DownloadsTextWriter
import java.time.LocalDate

internal class DayCheckInExporter(private val context: Context) {
    fun export(date: LocalDate, summaryText: String): String {
        return DownloadsTextWriter(context).write(dayCheckInFilename(date), "text/plain", summaryText)
    }
}

internal fun dayCheckInFilename(date: LocalDate): String = "meals_check_in_$date.txt"
