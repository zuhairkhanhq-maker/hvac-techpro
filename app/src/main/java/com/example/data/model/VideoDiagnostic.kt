package com.example.data.model

data class VideoDiagnosticSample(
    val id: String,
    val title: String,
    val equipmentType: String,
    val brand: String,
    val description: String,
    val simulatedPulses: Int,
    val pulseFrequencyHz: Double,
    val lightColor: String,
    val matchedCode: String,
    val faultSummary: String,
    val multimeterRoadmap: List<String>,
    val verifiedFix: String
)

object VideoDiagnosticDatabase {
    // Generic patterns deliberately avoid unverified brand-to-fault mappings.
    val samples = listOf(
        pattern("three_flashes", "Three flashes and a pause", 3, 1.2, "RED"),
        pattern("rapid_flashes", "Rapid continuous flashes", 12, 3.5, "AMBER"),
        pattern("four_flashes", "Four flashes and a pause", 4, 0.8, "GREEN")
    )
    private fun pattern(id: String, title: String, pulses: Int, frequency: Double, color: String) =
        VideoDiagnosticSample(id, title, "Control board", "Generic pattern", "Illustrative blink pattern only.",
            pulses, frequency, color, "No equipment code assigned",
            "Count flashes, note the pause and LED color, and record the exact model and control-board revision before consulting its manual.",
            listOf("Use the exact manufacturer service procedure."), "No repair recommendation provided.")
}
