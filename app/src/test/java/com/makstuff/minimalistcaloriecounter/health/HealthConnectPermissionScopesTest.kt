package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.permission.HealthPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectPermissionScopesTest {
    @Test
    fun `core connection status excludes optional full export permissions`() {
        val core = healthConnectPermissionsFor(HealthConnectPermissionScope.CoreAppFeatures)

        assertEquals(coreHealthConnectPermissions, core)
        assertEquals(core, defaultHealthConnectPermissions)
        assertFalse(core.contains(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY))
        assertFalse(defaultHealthConnectPermissions.contains(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY))
        assertFalse(core.containsAll(allReadPermissions))
    }

    @Test
    fun `nutrition mutation still requires read and write for duplicate-safe changes`() {
        val mutation = healthConnectPermissionsFor(HealthConnectPermissionScope.MutateNutritionRecords)

        assertTrue(mutation.containsAll(healthConnectNutritionReadPermissions))
        assertTrue(mutation.containsAll(healthConnectNutritionWritePermissions))
    }
}
