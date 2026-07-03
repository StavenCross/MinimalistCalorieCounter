package com.makstuff.minimalistcaloriecounter.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsSheetTest {
    @Test
    fun fromKeyAcceptsStableKeysAndEnumNames() {
        assertEquals(SettingsSheet.HealthData, SettingsSheet.fromKey("health_data"))
        assertEquals(SettingsSheet.ImportTools, SettingsSheet.fromKey("ImportTools"))
        assertEquals(SettingsSheet.Theme, SettingsSheet.fromKey("theme"))
        assertEquals(SettingsSheet.Language, SettingsSheet.fromKey("LANGUAGE"))
    }

    @Test
    fun fromKeyReturnsNullForMissingOrUnknownValues() {
        assertNull(SettingsSheet.fromKey(null))
        assertNull(SettingsSheet.fromKey(""))
        assertNull(SettingsSheet.fromKey("not_a_sheet"))
    }
}
