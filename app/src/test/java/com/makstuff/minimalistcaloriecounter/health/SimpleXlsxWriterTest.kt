package com.makstuff.minimalistcaloriecounter.health

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class SimpleXlsxWriterTest {
    @Test
    fun buildCreatesWorkbookPartsAndEscapesCells() {
        val bytes = SimpleXlsxWriter.build(
            listOf(
                XlsxSheet("Summary", listOf(listOf("field", "value"), listOf("name", "rice & tuna"))),
                XlsxSheet("Meals Foods", listOf(listOf("food", "kcal"), listOf("bread <slice>", "182"))),
            )
        )
        val entries = unzipEntries(bytes)

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("Meals Foods"))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("rice &amp; tuna"))
        assertTrue(entries.getValue("xl/worksheets/sheet2.xml").contains("bread &lt;slice&gt;"))
    }

    private fun unzipEntries(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        return entries
    }
}
