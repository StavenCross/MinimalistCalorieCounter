package com.makstuff.minimalistcaloriecounter.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val ROLLOVER_REFRESH_WORK = "nutrition_widget_rollover_refresh"

class NutritionWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val widgetCount = NutritionWidgetUpdater.updateAll(applicationContext)
            if (shouldScheduleNextWidgetRefresh(widgetCount)) {
                NutritionWidgetRefreshScheduler.scheduleNext(applicationContext)
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}

object NutritionWidgetRefreshScheduler {
    fun scheduleNext(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val request = OneTimeWorkRequestBuilder<NutritionWidgetRefreshWorker>()
            .setInitialDelay(delayUntilNextRolloverMillis(now), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            ROLLOVER_REFRESH_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Stops the self-rescheduling rollover job when the final widget is removed. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(ROLLOVER_REFRESH_WORK)
    }
}

internal fun shouldScheduleNextWidgetRefresh(widgetCount: Int): Boolean = widgetCount > 0

internal fun delayUntilNextRolloverMillis(now: ZonedDateTime): Long {
    val nextRollover = now.toLocalDate()
        .plusDays(1)
        .atStartOfDay(now.zone)
        .plusMinutes(2)
    return Duration.between(now, nextRollover).toMillis().coerceAtLeast(1L)
}
