package com.example.data.local

import com.example.data.model.TroubleshootingLog
import kotlinx.coroutines.flow.Flow

class TroubleshootingRepository(private val dao: TroubleshootingDao) {
    val allLogs: Flow<List<TroubleshootingLog>> = dao.getAllLogs()

    fun searchLogs(query: String): Flow<List<TroubleshootingLog>> = dao.searchLogs(query)

    suspend fun insertLog(log: TroubleshootingLog): Long = dao.insertLog(log)

    suspend fun updateLog(log: TroubleshootingLog) = dao.updateLog(log)

    suspend fun deleteLogById(id: Long) = dao.deleteLogById(id)

    suspend fun ensureInitialData() = dao.seedIfEmpty()
    suspend fun toggleFavorite(id: Long) = dao.toggleFavorite(id)
}
