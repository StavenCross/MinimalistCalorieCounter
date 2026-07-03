package com.makstuff.minimalistcaloriecounter.health

import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectExportCsvTest {
    @Test
    fun buildEscapesCommasQuotesAndNewlines() {
        val csv = HealthConnectExportCsv.build(
            listOf(
                listOf(
                    "NutritionRecord",
                    "2026-07-03T12:00:00Z",
                    "2026-07-03T12:01:00Z",
                    "record-1",
                    "client-1",
                    "1",
                    "com.example",
                    "1",
                    "2026-07-03T12:02:00Z",
                    "rice, chicken \"bowl\"",
                    "2",
                    "508",
                    "56",
                    "51.9",
                    "6",
                    "2",
                    "",
                    "",
                    "",
                    "",
                    "line one\nline two",
                )
            )
        )

        assertTrue(csv.startsWith("record_type,start_time,end_time"))
        assertTrue(csv.contains("\"rice, chicken \"\"bowl\"\"\""))
        assertTrue(csv.contains("\"line one\nline two\""))
    }
}
