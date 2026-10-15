package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FrostCyanLight
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.HvacBlue

enum class UserTier(
    val title: String,
    val badgeLabel: String,
    val priceDescription: String,
    val tagLine: String,
    val canAccessCalculators: Boolean = true,
    val canAccessPTCharts: Boolean = true,
    val canAccessBrainDump: Boolean = true,
    val canAccessA2LCompliance: Boolean = false,
    val canAccessErrorCodes: Boolean = false,
    val canExportWorksheets: Boolean = false,
    val canAccessDataPlateScanner: Boolean = false,
    val canAccessAftermarketMatcher: Boolean = false,
    val canAccessSchematicHighlighter: Boolean = false,
    val canAccessSeniorTechAI: Boolean = false,
    val canAccessSchematicScanner: Boolean = false,
    val equipmentLimit: Int = 3,
    val jobLimit: Int = 10,
    val canCreateReports: Boolean = false,
    val canUseBranding: Boolean = false,
    val canUseAdvancedWorkflows: Boolean = false,
    val canUseInspectionTemplates: Boolean = false,
    val canUseMaintenanceReminders: Boolean = false,
    val canUseEncryptedBackup: Boolean = false,
    val canUseBusinessDashboard: Boolean = false,
    val weeklyVideoScansAllowed: Int = 3
) {
    NEW_USER(
        title = "Free · Getting started",
        badgeLabel = "FREE",
        priceDescription = "Free Forever ($0)",
        tagLine = "Calculator estimates, reference tables and service notes.",
        canAccessA2LCompliance = false,
        canAccessErrorCodes = false,
        canExportWorksheets = false,
        canAccessAftermarketMatcher = false,
        canAccessSchematicHighlighter = false,
        canAccessSeniorTechAI = false,
        canAccessSchematicScanner = false,
        weeklyVideoScansAllowed = 3
    ),
    REGULAR_USER(
        title = "Free",
        badgeLabel = "FREE",
        priceDescription = "Free Forever ($0)",
        tagLine = "Clean daily dashboard + 100% offline local fix logbook.",
        canAccessA2LCompliance = false,
        canAccessErrorCodes = false,
        canExportWorksheets = false,
        canAccessDataPlateScanner = false,
        canAccessAftermarketMatcher = false,
        canAccessSchematicHighlighter = false,
        canAccessSeniorTechAI = false,
        canAccessSchematicScanner = false,
        weeklyVideoScansAllowed = 3
    ),
    PLUS_USER(
        title = "Plus",
        badgeLabel = "PLUS",
        priceDescription = "$19.99/month",
        tagLine = "Unlimited jobs, equipment history, OCR and service reports.",
        canAccessA2LCompliance = true,
        canAccessErrorCodes = true,
        canExportWorksheets = true,
        canAccessAftermarketMatcher = false,
        canAccessSchematicHighlighter = false,
        canAccessSeniorTechAI = true,
        canAccessDataPlateScanner = true,
        canAccessSchematicScanner = true,
        equipmentLimit = Int.MAX_VALUE,
        jobLimit = Int.MAX_VALUE,
        canCreateReports = true,
        weeklyVideoScansAllowed = 15
    ),
    PRO_USER(
        title = "Pro",
        badgeLabel = "PRO",
        priceDescription = "$49.99/month",
        tagLine = "Branded proof, reusable inspections, reminders, encrypted recovery and business insight.",
        canAccessA2LCompliance = true,
        canAccessErrorCodes = true,
        canExportWorksheets = true,
        canAccessDataPlateScanner = true,
        canAccessAftermarketMatcher = true,
        canAccessSchematicHighlighter = true,
        canAccessSeniorTechAI = true,
        canAccessSchematicScanner = true,
        equipmentLimit = Int.MAX_VALUE,
        jobLimit = Int.MAX_VALUE,
        canCreateReports = true,
        canUseBranding = true,
        canUseAdvancedWorkflows = true,
        canUseInspectionTemplates = true,
        canUseMaintenanceReminders = true,
        canUseEncryptedBackup = true,
        canUseBusinessDashboard = true,
        weeklyVideoScansAllowed = Int.MAX_VALUE
    );

    val badgeColor: Color
        get() = when (this) {
            NEW_USER -> FrostCyanLight
            REGULAR_USER -> HvacBlue
            PLUS_USER -> FlameOrange
            PRO_USER -> GaugeGreen
        }
}
