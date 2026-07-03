package com.makstuff.minimalistcaloriecounter.persistence

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppCsvStoreTest {
    @Test
    fun goalsRowsUseCsvParsingForQuotedCommas() {
        val file = File(createTempDirectory().toFile(), "goals.csv")
        file.writeText(
            """
            version,1
            recommendation,2026-07-03,1800.0,2400.0,"Calories low, review manually",1600.0,180.0,140.0,50.0,30.0
            """.trimIndent(),
        )

        val rows = AppCsvStore().readGoalsRows(file)
        val recommendation = rows.single { it.firstOrNull() == "recommendation" }

        assertEquals("Calories low, review manually", recommendation[4])
        assertEquals(10, recommendation.size)
    }

    @Test
    fun replaceCsvIfValidDoesNotOverwriteTargetWhenValidationFails() {
        val file = File(createTempDirectory().toFile(), "database.csv")
        file.writeText("existing")

        assertThrows(IllegalStateException::class.java) {
            AppCsvStore().replaceCsvIfValid(file, listOf(listOf("new"))) {
                error("invalid import")
            }
        }

        assertEquals("existing", file.readText())
    }
}
