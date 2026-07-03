package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.makstuff.minimalistcaloriecounter.classes.HealthConnectGoalSnapshot
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal class HealthConnectGoalProfileReader(
    private val client: HealthConnectClient,
) {
    suspend fun readSnapshot(): HealthConnectGoalProfileReadResult {
        return try {
            val start = Instant.now().minus(3650, ChronoUnit.DAYS)
            val end = Instant.now().plus(1, ChronoUnit.DAYS)
            val weight = readLatest(WeightRecord::class, start, end)
            val height = readLatest(HeightRecord::class, start, end)
            val bodyFat = readLatest(BodyFatRecord::class, start, end)
            val leanMass = readLatest(LeanBodyMassRecord::class, start, end)

            HealthConnectGoalProfileReadResult.Success(
                HealthConnectGoalSnapshot(
                    weightKg = weight?.weight?.inKilograms,
                    weightUpdatedAt = weight?.time?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) },
                    heightCm = height?.height?.inMeters?.times(100.0),
                    heightUpdatedAt = height?.time?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) },
                    bodyFatPercent = bodyFat?.percentage?.value,
                    bodyFatUpdatedAt = bodyFat?.time?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) },
                    leanMassKg = leanMass?.mass?.inKilograms,
                    leanMassUpdatedAt = leanMass?.time?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) },
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectGoalProfileReadResult.Failed(e.message ?: "Unknown Health Connect profile read error")
        }
    }

    private suspend fun <T : androidx.health.connect.client.records.Record> readLatest(
        recordType: kotlin.reflect.KClass<T>,
        start: Instant,
        end: Instant,
    ): T? {
        return client.readRecords(
            ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            )
        ).records.firstOrNull()
    }
}
