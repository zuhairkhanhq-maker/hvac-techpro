package com.example.data.remote

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SeniorTechAiService {
    suspend fun diagnoseFieldIssue(userQuery: String): String = withContext(Dispatchers.Default) {
        val q = userQuery.lowercase()
        val advice = when {
            listOf("gas smell", "smell gas", "gas odor").any(q::contains) -> "Gas odor: leave the area. Avoid ignition sources and switches. Contact the gas emergency service from a safe location. Do not continue diagnostics."
            listOf("head pressure", "high pressure", "high side").any(q::contains) -> "High head pressure: record ambient conditions, airflow, pressures and temperatures. A pressure reading alone cannot distinguish charge, airflow or contamination issues. Use the exact equipment service procedure."
            listOf("superheat", "subcool", "txv").any(q::contains) -> "Record superheat and subcooling after checking airflow and operating conditions. Blends need dew temperature for superheat and bubble temperature for subcooling. Compare with the exact charging chart; readings alone do not prove a leak or restriction."
            listOf("compressor", "trip", "hum", "buzz", "breaker").any(q::contains) -> "Compressor starting: do not repeatedly reset protection. Record model and fault pattern. Electrical isolation, stored-energy discharge and tests require qualified personnel following the equipment service manual."
            listOf("reversing", "o/b", "heat pump").any(q::contains) -> "Reversing valve: verify the model's control sequence and thermostat configuration. Energization logic differs by equipment; temperature alone does not confirm a failed valve."
            listOf("furnace", "flame", "ignit").any(q::contains) -> "Record furnace model and exact fault code before resetting. Follow the manufacturer's ignition sequence and flame-signal specifications. Never bypass flame, pressure or limit safety controls."
            listOf("freeze", "frozen", "ice", "coil").any(q::contains) -> "Coil icing: stop cooling and follow the equipment service instructions. Check accessible filters and blocked airflow. Do not chip ice or use a torch; reassess after thawing."
            else -> "No specific guide matched. Record the full manufacturer/model, fault code, mode and measurements. Consult that model's service manual. This guide does not understand arbitrary questions or remember prior messages."
        }
        "OFFLINE REFERENCE GUIDE\n\n$advice\n\nKeyword-selected checklist, not an AI diagnosis. It cannot verify equipment condition or prescribe a repair."
    }
    suspend fun scanSchematicOrDataPlate(bitmap: Bitmap?, samplePresetTitle: String = ""): String {
        require(bitmap != null) { "Choose a clear label photo. Preset examples are not scans." }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val task = recognizer.process(InputImage.fromBitmap(bitmap, 0))
            task.addOnCompleteListener { recognizer.close() }
            val result = task.await()
            require(result.text.isNotBlank()) { "No readable text. Retake the photo in focus and without glare." }
            return "EXTRACTED TEXT — VERIFY AGAINST THE PHOTO\n\n${result.text}\n\nOCR may misread digits and units. No part compatibility or circuit path has been inferred."
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { recognizer.close(); throw e }
    }
}
