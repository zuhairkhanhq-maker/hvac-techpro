package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "troubleshooting_logs")
data class TroubleshootingLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val brand: String,
    val equipmentType: String, // Split AC, Heat Pump, Gas Furnace, Package Unit, Mini-Split
    val symptom: String,
    val rootCause: String,
    val resolution: String,
    val errorCode: String = "",
    val refrigerantType: String = "",
    val superheatObserved: String = "",
    val subcoolingObserved: String = "",
    val customerTag: String = "", // e.g. "Main St. Bakery rooftop"
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPresetCheatSheet: Boolean = false
)
