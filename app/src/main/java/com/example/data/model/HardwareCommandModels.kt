package com.example.data.model

import androidx.compose.ui.graphics.Color

data class DataPlateSpec(
    val id: String,
    val unitTitle: String,
    val brand: String,
    val modelNumber: String,
    val serialNumber: String,
    val nominalCapacity: String, // e.g. "3.0 Tons / 36,000 BTU"
    val refrigerantType: String, // "R-410A" or "R-454B"
    val factoryCharge: String,   // "7 lbs 4 oz"
    val voltageRating: String,   // "208/230V, 1-Ph, 60Hz"
    val compressorRla: Double,
    val compressorLra: Double,
    val fanMotorFla: Double,
    val fanMotorHp: String,
    val fanMotorRpm: Int,
    val capacitorSpec: String,   // "45 / 5 µF @ 440 VAC"
    val mca: Double,             // Min Circuit Ampacity
    val mocp: Int,               // Max Overcurrent Protection
    val universalParts: List<UniversalPartMatch>
)

data class UniversalPartMatch(
    val category: String, // Dual-Run Capacitor, Fan Motor, Contactor, Hard Start Kit, Transformer
    val oemNumber: String,
    val universalReplacement: String,
    val manufacturer: String,
    val wiringNote: String,
    val truckStockStatus: String = "Common Truck Inventory"
)

data class SchematicCircuitPath(
    val id: String,
    val name: String,
    val voltageClass: String, // "24V Low-Voltage Control", "240V High-Voltage Power", "Motor Windings"
    val traceColor: Color,
    val wireNodes: List<String>,
    val meterTestPoint: String,
    val expectedReading: String,
    val commonFailure: String
)

object HardwareDatabase {
    val sampleDataPlates = listOf(
        DataPlateSpec(
            id = "carrier_24acc636",
            unitTitle = "Carrier Comfort 16 AC (3.0 Ton)",
            brand = "Carrier / Bryant",
            modelNumber = "24ACC636A003",
            serialNumber = "1822E49102",
            nominalCapacity = "3.0 Tons (36,000 BTU/h)",
            refrigerantType = "R-410A (Puron)",
            factoryCharge = "7.25 lbs (116 oz)",
            voltageRating = "208/230 VAC, 1 Phase, 60 Hz",
            compressorRla = 16.7,
            compressorLra = 79.0,
            fanMotorFla = 1.1,
            fanMotorHp = "1/8 HP",
            fanMotorRpm = 825,
            capacitorSpec = "45 / 5 µF @ 440 VAC",
            mca = 22.0,
            mocp = 35,
            universalParts = listOf(
                UniversalPartMatch(
                    category = "Dual-Run Capacitor",
                    oemNumber = "P291-4554RS",
                    universalReplacement = "Turbo 200 Multi-Tap (45+5 µF)",
                    manufacturer = "Global The Air (Turbo 200)",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                ),
                UniversalPartMatch(
                    category = "Condenser Fan Motor",
                    oemNumber = "HC33GE233",
                    universalReplacement = "Rescue Universal 5462 (1/8 - 1/3 HP, 825/1075 RPM)",
                    manufacturer = "US Motors / Nidec",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                ),
                UniversalPartMatch(
                    category = "Definite Purpose Contactor",
                    oemNumber = "HN51KC024",
                    universalReplacement = "Mars 61345 (2-Pole, 30A, 24V Coil)",
                    manufacturer = "Mars",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                ),
                UniversalPartMatch(
                    category = "Hard Start Kit",
                    oemNumber = "KSAHS2501AAA",
                    universalReplacement = "5-2-1 Compressor Saver CSRU1",
                    manufacturer = "CPS Products",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                )
            )
        ),
        DataPlateSpec(
            id = "goodman_gsx14036",
            unitTitle = "Goodman 14 SEER Condenser (3.0 Ton)",
            brand = "Goodman / Amana",
            modelNumber = "GSX140361KE",
            serialNumber = "2006781299",
            nominalCapacity = "3.0 Tons (36,000 BTU/h)",
            refrigerantType = "R-410A",
            factoryCharge = "6.80 lbs (109 oz)",
            voltageRating = "208/230 VAC, 1 Phase, 60 Hz",
            compressorRla = 14.1,
            compressorLra = 77.0,
            fanMotorFla = 0.9,
            fanMotorHp = "1/6 HP",
            fanMotorRpm = 850,
            capacitorSpec = "40 / 5 µF @ 440 VAC",
            mca = 18.5,
            mocp = 30,
            universalParts = listOf(
                UniversalPartMatch(
                    category = "Dual-Run Capacitor",
                    oemNumber = "CAP050400440RTP",
                    universalReplacement = "Titan HD 40/5 µF 440V Round",
                    manufacturer = "Packard / Titan Pro",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                ),
                UniversalPartMatch(
                    category = "Condenser Fan Motor",
                    oemNumber = "0131M00007PS",
                    universalReplacement = "Fasco D7909 Multi-Horsepower",
                    manufacturer = "Fasco",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                ),
                UniversalPartMatch(
                    category = "Contactor",
                    oemNumber = "CONT1P025024V",
                    universalReplacement = "Packard C130A (1-Pole + Shunt, 30A, 24V)",
                    manufacturer = "Packard",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                )
            )
        ),
        DataPlateSpec(
            id = "carrier_454b_puron_advance",
            unitTitle = "2026 Carrier Next-Gen A2L Condenser",
            brand = "Carrier",
            modelNumber = "24SCA536A003",
            serialNumber = "2601P94301",
            nominalCapacity = "3.0 Tons (36,000 BTU/h)",
            refrigerantType = "R-454B (Puron Advance / A2L)",
            factoryCharge = "6.10 lbs (98 oz)",
            voltageRating = "208/230 VAC, 1 Phase, 60 Hz",
            compressorRla = 15.2,
            compressorLra = 74.0,
            fanMotorFla = 1.0,
            fanMotorHp = "1/8 HP ECM",
            fanMotorRpm = 850,
            capacitorSpec = "45 µF @ 440 VAC (Single Run / ECM Fan)",
            mca = 20.0,
            mocp = 30,
            universalParts = listOf(
                UniversalPartMatch(
                    category = "Compressor Run Capacitor",
                    oemNumber = "P291-4504R",
                    universalReplacement = "Titan Pro 45 µF 440V Oval/Round",
                    manufacturer = "Packard",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                ),
                UniversalPartMatch(
                    category = "A2L Dissipation Board Fuse",
                    oemNumber = "FUS01003",
                    universalReplacement = "3A Fast-Acting Automotive ATC Blade Fuse",
                    manufacturer = "Bussmann",
                    wiringNote = "Unverified example. Consult the exact equipment manufacturer instructions."
                )
            )
        )
    )

    val schematicCircuits = listOf(
        SchematicCircuitPath(
            id = "circuit_cooling_call",
            name = "24V Low-Voltage Cooling Call (Y - C)",
            voltageClass = "24V Control",
            traceColor = Color(0xFF38BDF8), // Electric Cyan
            wireNodes = listOf(
                "Thermostat 'R' (24VAC Power from Transformer)",
                "Thermostat 'Y' Contact (Cooling Demand)",
                "Outdoor Defrost / Safety Board 'Y_IN'",
                "High Pressure Cutout Switch (Normally Closed)",
                "Low Pressure / Loss of Charge Switch (Normally Closed)",
                "Contactor 24V Coil Terminal 1",
                "Common 'C' Wire back to Transformer 24V Common"
            ),
            meterTestPoint = "Unverified example. Consult the exact equipment manufacturer instructions.",
            expectedReading = "Unverified example. Consult the exact equipment manufacturer instructions.",
            commonFailure = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        SchematicCircuitPath(
            id = "circuit_line_power",
            name = "240V Line Voltage Supply (L1 - L2)",
            voltageClass = "240V Power",
            traceColor = Color(0xFFEF4444), // Danger Red
            wireNodes = listOf(
                "Outdoor Disconnect Switch (Line 1 & Line 2)",
                "Contactor Input Terminal L1 & L2",
                "Contactor Movable Copper Bridge Contacts",
                "Contactor Output Terminal T1 & T2",
                "Compressor 'Common' & Fan Motor 'Black'"
            ),
            meterTestPoint = "Unverified example. Consult the exact equipment manufacturer instructions.",
            expectedReading = "Unverified example. Consult the exact equipment manufacturer instructions.",
            commonFailure = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        SchematicCircuitPath(
            id = "circuit_motor_windings",
            name = "Compressor Motor Run & Start Circuit",
            voltageClass = "Motor Windings",
            traceColor = Color(0xFFFBBF24), // Amber Gold
            wireNodes = listOf(
                "Contactor T1 -> Compressor Common (C)",
                "Contactor T2 -> Dual-Run Capacitor 'C' (Common)",
                "Capacitor 'C' -> Compressor Run Winding (R)",
                "Capacitor 'HERM' -> Compressor Start Winding (S)"
            ),
            meterTestPoint = "Unverified example. Consult the exact equipment manufacturer instructions.",
            expectedReading = "Unverified example. Consult the exact equipment manufacturer instructions.",
            commonFailure = "Unverified example. Consult the exact equipment manufacturer instructions."
        ),
        SchematicCircuitPath(
            id = "circuit_reversing_valve",
            name = "Reversing Valve Circuit (O / B Terminal)",
            voltageClass = "Heat Pump Valve",
            traceColor = Color(0xFF10B981), // Emerald Green
            wireNodes = listOf(
                "Thermostat 'O' (Cooling Energized: Carrier/Goodman/Trane)",
                "or Thermostat 'B' (Heating Energized: Rheem/Ruud)",
                "Defrost Control Board Valve Relay",
                "Reversing Valve 24V Solenoid Coil",
                "24V Common (C)"
            ),
            meterTestPoint = "Unverified example. Consult the exact equipment manufacturer instructions.",
            expectedReading = "Unverified example. Consult the exact equipment manufacturer instructions.",
            commonFailure = "Unverified example. Consult the exact equipment manufacturer instructions."
        )
    )
}
