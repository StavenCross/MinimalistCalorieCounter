package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneId

internal class HealthConnectGoalProfileWriter(
    private val client: HealthConnectClient,
) {
    suspend fun writeManualMeasurements(
        heightCm: Double? = null,
        weightKg: Double? = null,
    ): HealthConnectGoalProfileWriteResult {
        return try {
            val now = Instant.now()
            val offset = ZoneId.systemDefault().rules.getOffset(now)
            val recordSuffix = now.toEpochMilli()
            val records = buildList {
                if (heightCm != null) {
                    add(
                        HeightRecord(
                            time = now,
                            zoneOffset = offset,
                            height = Length.meters(heightCm / 100.0),
                            metadata = Metadata.manualEntry("mcc-goal-height-$recordSuffix", 1L),
                        )
                    )
                }
                if (weightKg != null) {
                    add(
                        WeightRecord(
                            time = now,
                            zoneOffset = offset,
                            weight = Mass.kilograms(weightKg),
                            metadata = Metadata.manualEntry("mcc-goal-weight-$recordSuffix", 1L),
                        )
                    )
                }
            }
            if (records.isNotEmpty()) client.insertRecords(records)
            HealthConnectGoalProfileWriteResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthConnectGoalProfileWriteResult.Failed(e.message ?: "Unknown Health Connect profile write error")
        }
    }
}
