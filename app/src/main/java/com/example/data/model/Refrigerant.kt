package com.example.data.model

import androidx.compose.ui.graphics.Color
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.roundToInt

data class Refrigerant(
    val id: String,
    val name: String,
    val cylinderColorHex: Long,
    val safetyClass: String, // A1, A2L
    val chemicalType: String, // HFC, HFO Blend
    val gwp: Int,
    val glideF: Double,
    val description: String,
    val typicalEvapPressurePsig: Int,
    val typicalCondPressurePsig: Int,
    val defaultTargetSuperheatF: Double,
    val defaultTargetSubcoolingF: Double,
    // Antoine-like coefficients for Saturation Temp (°F) from Pressure (psig)
    // T_F = C1 + C2 * ln(P + 14.7) + C3 * (ln(P + 14.7))^2
    private val c1: Double,
    private val c2: Double,
    private val c3: Double,
    // Pressure lookup points for PT chart table
    val samplePoints: List<Pair<Int, Double>> // psig to tempF
) {
    /**
     * Converts gauge pressure (psig) to Saturated Vapor Temperature (°F)
     */
    fun satTempFromPsig(psig: Double): Double {
        require(samplePoints.size >= 2) { "PT table unavailable" }
        val sorted = samplePoints.sortedBy { it.first }
        require(psig.isFinite() && psig in sorted.first().first.toDouble()..sorted.last().first.toDouble()) { "Pressure outside reference range: ${sorted.first().first}–${sorted.last().first} psig" }
        if (psig <= sorted.first().first) {
            val (p0, t0) = sorted[0]
            val (p1, t1) = sorted[1]
            val slope = (t1 - t0) / (p1 - p0)
            val res = t0 + slope * (psig - p0)
            return (res * 10.0).roundToInt() / 10.0
        }
        if (psig >= sorted.last().first) {
            val (p0, t0) = sorted[sorted.size - 2]
            val (p1, t1) = sorted.last()
            val slope = (t1 - t0) / (p1 - p0)
            val res = t1 + slope * (psig - p1)
            return (res * 10.0).roundToInt() / 10.0
        }
        for (i in 0 until sorted.size - 1) {
            val (p0, t0) = sorted[i]
            val (p1, t1) = sorted[i + 1]
            if (psig in p0.toDouble()..p1.toDouble()) {
                val ratio = (psig - p0) / (p1 - p0)
                val res = t0 + ratio * (t1 - t0)
                return (res * 10.0).roundToInt() / 10.0
            }
        }
        return sorted.last().second
    }

    /**
     * Converts Temperature (°F) to saturated gauge pressure (psig)
     */
    fun psigFromTempF(tempF: Double): Double {
        require(samplePoints.size >= 2) { "PT table unavailable" }
        val sorted = samplePoints.sortedBy { it.second }
        require(tempF.isFinite() && tempF in sorted.first().second..sorted.last().second) { "Temperature outside reference range" }
        if (tempF <= sorted.first().second) {
            val (p0, t0) = sorted[0]
            val (p1, t1) = sorted[1]
            val slope = (p1 - p0).toDouble() / (t1 - t0)
            val res = p0 + slope * (tempF - t0)
            return (res * 10.0).roundToInt() / 10.0
        }
        if (tempF >= sorted.last().second) {
            val (p0, t0) = sorted[sorted.size - 2]
            val (p1, t1) = sorted.last()
            val slope = (p1 - p0).toDouble() / (t1 - t0)
            val res = p1 + slope * (tempF - t1)
            return (res * 10.0).roundToInt() / 10.0
        }
        for (i in 0 until sorted.size - 1) {
            val (p0, t0) = sorted[i]
            val (p1, t1) = sorted[i + 1]
            if (tempF in t0..t1) {
                val ratio = (tempF - t0) / (t1 - t0)
                val res = p0 + ratio * (p1 - p0)
                return (res * 10.0).roundToInt() / 10.0
            }
        }
        return sorted.last().first.toDouble()
    }

    val displayColor: Color
        get() = Color(cylinderColorHex)
}

object RefrigerantData {
    val R410A = Refrigerant(
        id = "R-410A",
        name = "R-410A (Puron)",
        cylinderColorHex = 0xFFFF70A6, // Pink
        safetyClass = "A1 (Non-Flammable)",
        chemicalType = "HFC Blend (50/50 R-32/R-125)",
        gwp = 2088,
        glideF = 0.2,
        description = "Standard high-pressure residential/commercial AC refrigerant.",
        typicalEvapPressurePsig = 118,
        typicalCondPressurePsig = 365,
        defaultTargetSuperheatF = 12.0,
        defaultTargetSubcoolingF = 10.0,
        c1 = -186.4,
        c2 = 46.2,
        c3 = -0.58,
        samplePoints = listOf(
            70 to 14.0, 80 to 20.5, 90 to 26.5, 100 to 32.0, 110 to 37.0,
            118 to 40.0, 125 to 43.5, 135 to 48.0, 145 to 52.5, 200 to 71.0,
            250 to 85.0, 300 to 97.0, 340 to 105.0, 365 to 110.0, 400 to 117.0,
            420 to 120.5, 450 to 126.0
        )
    )

    val R454B = Refrigerant(
        id = "R-454B",
        name = "R-454B (Puron Advance / Opteon XL41)",
        cylinderColorHex = 0xFFEF4444, // Red / A2L Red band
        safetyClass = "A2L (Mildly Flammable)",
        chemicalType = "HFO Blend (68.9% R-32 / 31.1% R-1234yf)",
        gwp = 466,
        glideF = 1.5,
        description = "Next-generation EPA 2025/2026 low-GWP replacement for R-410A in new systems.",
        typicalEvapPressurePsig = 114,
        typicalCondPressurePsig = 352,
        defaultTargetSuperheatF = 12.0,
        defaultTargetSubcoolingF = 9.0,
        c1 = -188.0,
        c2 = 46.8,
        c3 = -0.65,
        samplePoints = listOf(
            68 to 14.0, 78 to 20.0, 88 to 26.0, 98 to 31.5, 108 to 37.0,
            114 to 40.0, 122 to 43.5, 132 to 48.0, 142 to 52.0, 195 to 70.0,
            245 to 84.0, 295 to 96.0, 335 to 104.5, 352 to 110.0, 390 to 116.5,
            415 to 120.0, 440 to 124.5
        )
    )

    val R22 = Refrigerant(
        id = "R-22",
        name = "R-22 (Freon)",
        cylinderColorHex = 0xFF10B981, // Green
        safetyClass = "A1 (Non-Flammable)",
        chemicalType = "HCFC (Chlorodifluoromethane)",
        gwp = 1810,
        glideF = 0.0,
        description = "Legacy AC refrigerant. Phased out of production, highly monitored.",
        typicalEvapPressurePsig = 69,
        typicalCondPressurePsig = 226,
        defaultTargetSuperheatF = 12.0,
        defaultTargetSubcoolingF = 10.0,
        c1 = -158.0,
        c2 = 48.5,
        c3 = -0.72,
        samplePoints = listOf(
            40 to 18.0, 45 to 22.0, 50 to 26.0, 58 to 32.5, 65 to 37.5,
            68 to 40.0, 75 to 44.5, 80 to 48.0, 90 to 54.0, 140 to 79.0,
            170 to 91.0, 200 to 102.0, 226 to 110.0, 250 to 117.0, 275 to 124.0
        )
    )

    val R32 = Refrigerant(
        id = "R-32",
        name = "R-32 (Difluoromethane)",
        cylinderColorHex = 0xFFF97316, // Orange / Red stripe
        safetyClass = "A2L (Mildly Flammable)",
        chemicalType = "Pure HFC (Single Component - Zero Glide)",
        gwp = 675,
        glideF = 0.0,
        description = "Single component A2L refrigerant used extensively by Daikin and Goodman.",
        typicalEvapPressurePsig = 122,
        typicalCondPressurePsig = 378,
        defaultTargetSuperheatF = 10.0,
        defaultTargetSubcoolingF = 9.0,
        c1 = -184.0,
        c2 = 45.8,
        c3 = -0.55,
        samplePoints = listOf(
            72 to 14.5, 83 to 21.0, 94 to 27.0, 105 to 33.0, 115 to 38.0,
            122 to 40.0, 130 to 44.0, 140 to 48.5, 150 to 53.0, 210 to 72.0,
            260 to 86.0, 310 to 98.0, 350 to 106.0, 378 to 110.0, 415 to 117.5
        )
    )

    val R134A = Refrigerant(
        id = "R-134a",
        name = "R-134a (Suva 134a)",
        cylinderColorHex = 0xFF06B6D4, // Light Blue
        safetyClass = "A1 (Non-Flammable)",
        chemicalType = "HFC (Tetrafluoroethane)",
        gwp = 1430,
        glideF = 0.0,
        description = "Medium pressure refrigerant used in chillers, dehumidifiers, and automotive AC.",
        typicalEvapPressurePsig = 35,
        typicalCondPressurePsig = 146,
        defaultTargetSuperheatF = 10.0,
        defaultTargetSubcoolingF = 10.0,
        c1 = -135.0,
        c2 = 45.0,
        c3 = -0.68,
        samplePoints = listOf(
            15 to 15.0, 20 to 22.5, 25 to 29.0, 30 to 35.0, 35 to 40.0,
            40 to 45.0, 45 to 49.5, 50 to 53.5, 70 to 69.0, 90 to 82.0,
            110 to 93.0, 130 to 102.5, 146 to 110.0, 160 to 116.0, 180 to 123.5
        )
    )

    val R404A = Refrigerant(
        id = "R-404A",
        name = "R-404A (HP62)",
        cylinderColorHex = 0xFFEA580C, // Deep Orange
        safetyClass = "A1 (Non-Flammable)",
        chemicalType = "HFC Blend",
        gwp = 3922,
        glideF = 0.8,
        description = "Low & medium temperature commercial walk-in freezers and coolers.",
        typicalEvapPressurePsig = 86,
        typicalCondPressurePsig = 278,
        defaultTargetSuperheatF = 8.0,
        defaultTargetSubcoolingF = 8.0,
        c1 = -172.0,
        c2 = 47.0,
        c3 = -0.66,
        samplePoints = listOf(
            15 to -18.0, 25 to -6.0, 35 to 4.0, 50 to 16.0, 65 to 26.5,
            86 to 40.0, 100 to 48.0, 120 to 58.0, 150 to 71.0, 200 to 90.0,
            240 to 102.0, 278 to 110.0, 320 to 122.0
        )
    )

    val allRefrigerants: List<Refrigerant> = listOf(R410A, R454B, R22, R32, R134A, R404A)
}
