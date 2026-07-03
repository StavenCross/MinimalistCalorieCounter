package com.makstuff.minimalistcaloriecounter.health

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

internal const val HEALTH_CONNECT_QUOTA_RETRY_ATTEMPTS = 5

internal fun Throwable.isHealthConnectQuotaError(): Boolean {
    return message?.contains("quota exceeded", ignoreCase = true) == true
}

internal fun healthConnectQuotaRetryDelayMillis(attempt: Int): Long = 3000L * attempt

internal suspend fun retryHealthConnectQuotaSensitive(
    maxAttempts: Int = HEALTH_CONNECT_QUOTA_RETRY_ATTEMPTS,
    block: suspend () -> Unit,
) {
    var attempts = 0
    while (true) {
        try {
            yield()
            block()
            return
        } catch (e: Throwable) {
            attempts++
            if (e.isHealthConnectQuotaError() && attempts < maxAttempts) {
                delay(healthConnectQuotaRetryDelayMillis(attempts).milliseconds)
            } else {
                throw e
            }
        }
    }
}
