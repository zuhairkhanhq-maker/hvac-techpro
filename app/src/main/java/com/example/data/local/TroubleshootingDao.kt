package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TroubleshootingLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TroubleshootingDao {
    @Query("SELECT * FROM troubleshooting_logs ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllLogs(): Flow<List<TroubleshootingLog>>

    @Query("SELECT * FROM troubleshooting_logs WHERE id = :id")
    suspend fun getLogById(id: Long): TroubleshootingLog?

    @Query("SELECT * FROM troubleshooting_logs WHERE brand LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR symptom LIKE '%' || :query || '%' OR errorCode LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<TroubleshootingLog>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: TroubleshootingLog): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(logs: List<TroubleshootingLog>)

    @Query("UPDATE troubleshooting_logs SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT COUNT(*) FROM troubleshooting_logs WHERE isPresetCheatSheet = 1")
    suspend fun getPresetCount(): Int

    @androidx.room.Transaction
    suspend fun seedIfEmpty() { if (getPresetCount() == 0) insertAll(AppDatabase.INITIAL_CHEAT_SHEETS) }

    @Update
    suspend fun updateLog(log: TroubleshootingLog)

    @Query("DELETE FROM troubleshooting_logs WHERE id = :id AND isPresetCheatSheet = 0")
    suspend fun deleteLogById(id: Long)

    @Query("SELECT COUNT(*) FROM troubleshooting_logs")
    suspend fun getCount(): Int
}
