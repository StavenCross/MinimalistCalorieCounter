package com.makstuff.minimalistcaloriecounter.persistence

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupRulesTest {
    @Test
    fun `backup rules include room database and mirrored csv files`() {
        val backupRules = File("src/main/res/xml/backup_rules.xml").readText()
        val dataExtractionRules = File("src/main/res/xml/data_extraction_rules.xml").readText()

        listOf(
            "archive.csv",
            "database.csv",
            "day.csv",
            "goals.csv",
            "options.csv",
            "quick_import_outbox.csv",
            "mcc.db",
            "mcc.db-shm",
            "mcc.db-wal",
        ).forEach { path ->
            assertTrue("$path missing from backup_rules.xml", backupRules.contains(path))
            assertTrue("$path missing from data_extraction_rules.xml", dataExtractionRules.contains(path))
        }
    }
}
