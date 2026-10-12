package com.example.data.model

data class ErrorCode(
    val id: String,
    val brand: String,
    val equipmentCategory: String, // Gas Furnace, Heat Pump, Inverter / Mini-Split, Package Unit
    val codeDisplay: String, // "3 Red Flashes", "Code 33", "E1 Error"
    val flashPattern: String, // "Slow pulse x 3, 2-sec pause"
    val faultTitle: String,
    val severity: ErrorSeverity,
    val probableCauses: List<String>,
    val fieldDiagnosticChecks: List<String>,
    val verifiedFix: String,
    val boardTerminalNote: String = ""
)

enum class ErrorSeverity {
    HARD_LOCKOUT,
    SOFT_LOCKOUT,
    WARNING,
    DIAGNOSTIC_INFO
}

object ErrorCodeDatabase {
    val entries: List<ErrorCode> = listOf(
        // GOODMAN / AMANA
        ErrorCode(
            id = "goodman_3_flashes",
            brand = "Goodman / Amana",
            equipmentCategory = "Gas Furnace",
            codeDisplay = "3 Red Flashes",
            flashPattern = "● ● ● (3 red pulses, 2s pause)",
            faultTitle = "Pressure Switch Stuck Open",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Blocked or pinched rubber tubing between inducer and switch",
                "Clogged condensate collector box or trap (90%+ high-efficiency units)",
                "Flue vent pipe obstructed by birds, snow, or excessive elbows",
                "Weak draft inducer motor not pulling sufficient negative w.c. draft"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        ErrorCode(
            id = "goodman_1_flash",
            brand = "Goodman / Amana",
            equipmentCategory = "Gas Furnace",
            codeDisplay = "1 Red Flash",
            flashPattern = "● (1 pulse every 3 seconds)",
            faultTitle = "System in 1-Hour Lockout (Ignition Failure)",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Manual gas shutoff valve closed",
                "Dirty flame sensor rod",
                "Low gas manifold pressure (<3.5\" w.c. natural gas)",
                "Cracked hot surface igniter element"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),

        // CARRIER / BRYANT
        ErrorCode(
            id = "carrier_33",
            brand = "Carrier / Bryant",
            equipmentCategory = "Gas Furnace",
            codeDisplay = "Code 33",
            flashPattern = "3 short flashes, 3 long flashes (LED pulse)",
            faultTitle = "High Limit Switch Open",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Extremely dirty 1\" pleated air filter restricting airflow",
                "Closed or blocked supply registers throughout house",
                "Blower capacitor weak or ECM motor failed",
                "Secondary heat exchanger clogged with pet hair/dirt"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        ErrorCode(
            id = "carrier_31",
            brand = "Carrier / Bryant",
            equipmentCategory = "Gas Furnace",
            codeDisplay = "Code 31",
            flashPattern = "3 short flashes, 1 long flash",
            faultTitle = "High-Heat Pressure Switch Did Not Close",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Restricted combustion air intake or flue pipe",
                "Defective 2-stage inducer motor relay or capacitor",
                "Water accumulated in inducer housing from failed drain tube"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),

        // TRANE / AMERICAN STANDARD
        ErrorCode(
            id = "trane_2_flashes",
            brand = "Trane / American Standard",
            equipmentCategory = "Heat Pump",
            codeDisplay = "2 Amber Flashes",
            flashPattern = "● ● (2 amber flashes)",
            faultTitle = "High Pressure Cutout Tripped (Head Pressure Trip)",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Outdoor condenser coil caked with cottonwood/grass clippings",
                "Outdoor condenser fan motor seized or bad capacitor",
                "Overcharged refrigerant system",
                "Non-condensables (air/nitrogen) in refrigerant line set"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        ErrorCode(
            id = "trane_4_flashes",
            brand = "Trane / American Standard",
            equipmentCategory = "Gas Furnace",
            codeDisplay = "4 Red Flashes",
            flashPattern = "● ● ● ● (4 red flashes)",
            faultTitle = "Thermal High Limit Switch Open Circuit",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Low airflow across primary heat exchanger",
                "ECM blower motor module burned out or loose 16-pin harness",
                "Duct static pressure exceeding 0.8\" w.c."
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),

        // LENNOX
        ErrorCode(
            id = "lennox_slow_flash",
            brand = "Lennox",
            equipmentCategory = "Gas Furnace",
            codeDisplay = "Slow Alternating Red/Green",
            flashPattern = "Red ON 1s, Green ON 1s alternating",
            faultTitle = "Reverse Polarity or Poor Ground Reference",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "120VAC line Hot (Black) and Neutral (White) reversed at furnace junction box",
                "Missing or loose ground wire to electrical earth ground rod",
                "Corroded control board mounting ground screw"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        ErrorCode(
            id = "lennox_e228",
            brand = "Lennox",
            equipmentCategory = "Inverter / Heat Pump",
            codeDisplay = "Error 228 (E228)",
            flashPattern = "Digital 7-segment display: 228",
            faultTitle = "Compressor Inverter Drive IPM Module Overcurrent",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Compressor windings grounded or shorted phase-to-phase",
                "Failed Inverter Power Module (IPM) on outdoor control board",
                "Loose U, V, W wiring connections to compressor harness"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),

        // DAIKIN / MITSUBISHI (MINI-SPLIT & INVERTERS)
        ErrorCode(
            id = "daikin_u4",
            brand = "Daikin",
            equipmentCategory = "Inverter / Mini-Split",
            codeDisplay = "U4 Fault Code",
            flashPattern = "Remote Controller display: U4",
            faultTitle = "Indoor to Outdoor Communication Transmission Error",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Broken or spliced 14/4 communication wire between indoor and outdoor units",
                "Wires crossed at terminal blocks (1 to 2 or 2 to 3)",
                "Severe electromagnetic noise induced from parallel high-voltage line",
                "Blown communication fuse on outdoor PCB"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        ErrorCode(
            id = "mitsubishi_e6",
            brand = "Mitsubishi Electric",
            equipmentCategory = "Inverter / Mini-Split",
            codeDisplay = "E6 / Flash 6",
            flashPattern = "Indoor LED blinks 6 times or display E6",
            faultTitle = "Serial Communication Transmission Failure",
            severity = ErrorSeverity.HARD_LOCKOUT,
            probableCauses = listOf(
                "Outdoor unit power disconnected or breaker tripped",
                "S3 terminal wire broken or loose",
                "Outdoor power board failure"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),

        // RHEEM / RUUD
        ErrorCode(
            id = "rheem_code_13",
            brand = "Rheem / Ruud",
            equipmentCategory = "Gas Furnace",
            codeDisplay = "Code 13 (Flash 13)",
            flashPattern = "1 green flash, 3 amber flashes",
            faultTitle = "Flame Lost After Ignition",
            severity = ErrorSeverity.SOFT_LOCKOUT,
            probableCauses = listOf(
                "Weak draft pulling flame away from sensor rod",
                "Low gas supply pressure dropping during high fire",
                "Dirty flame sensor",
                "Moisture on burner assembly"
            ),
            fieldDiagnosticChecks = listOf("Use the manufacturer service procedure for the exact model and board revision."),
            verifiedFix = "Unverified example. Consult the exact equipment manufacturer instructions.",
            boardTerminalNote = "Unverified example. Consult the exact equipment manufacturer instructions."
        )
    )
}
