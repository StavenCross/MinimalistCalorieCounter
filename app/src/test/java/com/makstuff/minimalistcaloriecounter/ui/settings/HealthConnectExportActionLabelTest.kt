package com.makstuff.minimalistcaloriecounter.ui.settings

import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectExportActionLabelTest {
    @Test
    fun showsPermissionActionBeforeExporting() {
        assertEquals(
            "Grant export read permissions",
            healthConnectExportActionLabel(
                mode = HealthConnectExportMode.Full,
                permissionsGranted = false,
                inProgress = false,
            ),
        )
    }

    @Test
    fun showsFullExportActionWhenPermissionsAreGranted() {
        assertEquals(
            "Export all to CSV",
            healthConnectExportActionLabel(
                mode = HealthConnectExportMode.Full,
                permissionsGranted = true,
                inProgress = false,
            ),
        )
    }

    @Test
    fun showsProgressStateDuringExport() {
        assertEquals(
            "Exporting Health Connect CSV...",
            healthConnectExportActionLabel(
                mode = HealthConnectExportMode.Full,
                permissionsGranted = true,
                inProgress = true,
            ),
        )
    }
}
