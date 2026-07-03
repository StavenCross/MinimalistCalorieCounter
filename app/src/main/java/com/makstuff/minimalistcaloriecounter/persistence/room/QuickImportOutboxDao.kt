package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface QuickImportOutboxDao {
    @Query("SELECT * FROM quick_import_outbox ORDER BY createdAt DESC")
    suspend fun listItems(): List<QuickImportOutboxEntity>

    @Query("SELECT * FROM quick_import_outbox_payloads WHERE outboxId = :outboxId ORDER BY payloadIndex")
    suspend fun payloads(outboxId: String): List<QuickImportOutboxPayloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: QuickImportOutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayloads(payloads: List<QuickImportOutboxPayloadEntity>)

    @Query("DELETE FROM quick_import_outbox_payloads WHERE outboxId = :outboxId")
    suspend fun clearPayloads(outboxId: String)

    @Transaction
    suspend fun upsert(seed: QuickImportOutboxRoomSeed) {
        upsertItem(seed.item)
        clearPayloads(seed.item.id)
        upsertPayloads(seed.payloads)
    }
}
