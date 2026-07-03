package com.makstuff.minimalistcaloriecounter.health

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectRetryTest {
    @Test
    fun detectsQuotaErrorsCaseInsensitively() {
        assertTrue(IllegalStateException("Quota exceeded").isHealthConnectQuotaError())
        assertTrue(RuntimeException("health connect QUOTA EXCEEDED, try later").isHealthConnectQuotaError())
        assertFalse(RuntimeException("permission denied").isHealthConnectQuotaError())
    }

    @Test
    fun quotaRetryDelayIncreasesByAttempt() {
        assertEquals(3000L, healthConnectQuotaRetryDelayMillis(1))
        assertEquals(6000L, healthConnectQuotaRetryDelayMillis(2))
        assertEquals(9000L, healthConnectQuotaRetryDelayMillis(3))
    }

    @Test
    fun quotaSensitiveRetryRetriesUntilSuccessful() = runTest {
        var attempts = 0

        retryHealthConnectQuotaSensitive(maxAttempts = 3) {
            attempts++
            if (attempts < 3) error("quota exceeded")
        }

        assertEquals(3, attempts)
    }

    @Test
    fun quotaSensitiveRetryDoesNotRetryOtherFailures() = runTest {
        var attempts = 0
        val failure = runCatching {
            retryHealthConnectQuotaSensitive(maxAttempts = 3) {
                attempts++
                error("permission denied")
            }
        }.exceptionOrNull()

        assertEquals(1, attempts)
        assertEquals("permission denied", failure?.message)
    }
}
