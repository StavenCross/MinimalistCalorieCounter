package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HealthConnectManifestPermissionsTest {
    @Test
    fun `manifest declares every full export read permission`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val permissions = exportRecordTypes
            .map { HealthPermission.getReadPermission(it) }
            .toSet() + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

        permissions.forEach { permission ->
            assertTrue("$permission missing from manifest", manifest.contains(permission))
        }
    }

    @Test
    fun `manifest declares manual goal profile write permissions`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        listOf(
            HealthPermission.getWritePermission(HeightRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
        ).forEach { permission ->
            assertTrue("$permission missing from manifest", manifest.contains(permission))
        }
    }
}
