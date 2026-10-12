package com.example.data.model

object NameplateParser {
    private val makers = listOf("Carrier", "Trane", "Lennox", "Daikin", "Mitsubishi", "Goodman", "Rheem", "York", "Bosch", "LG", "Samsung")
    private fun find(text: String, pattern: Regex): String = pattern.find(text)?.groupValues?.getOrNull(1)?.trim(' ', ':', '-', '#') ?: ""

    fun parse(raw: String): NameplateFields {
        val flat = raw.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        if (flat.isBlank()) return NameplateFields()
        val manufacturer = makers.firstOrNull { flat.contains(it, true) } ?: ""
        val model = find(flat, Regex("(?i)(?:model|mdl)\\s*(?:no\\.?|number|#)?\\s*[:#-]?\\s*([A-Z0-9][A-Z0-9._/-]{3,30})"))
        val serial = find(flat, Regex("(?i)(?:serial|ser|s/n)\\s*(?:no\\.?|number|#)?\\s*[:#-]?\\s*([A-Z0-9][A-Z0-9._/-]{3,30})"))
        val voltage = find(flat, Regex("(?i)(?:volts?|voltage)\\s*[:#-]?\\s*([0-9]{2,3}(?:/[0-9]{2,3})?)"))
        val phase = find(flat, Regex("(?i)(?:phase|ph)\\s*[:#-]?\\s*([123])"))
        val refrigerant = find(flat, Regex("(?i)\\b(R[- ]?(?:22|32|134A|290|404A|407C|410A|448A|449A|452B|454B|600A))\\b")).uppercase().replace(" ", "-")
        val capacity = find(flat, Regex("(?i)(?:capacity|cooling)\\s*[:#-]?\\s*([0-9,.]+\\s*(?:BTU/?H|BTUH|TONS?|KW))"))
        val found = listOf(manufacturer, model, serial, voltage, phase, refrigerant, capacity).count { it.isNotBlank() }
        return NameplateFields(manufacturer, model, serial, voltage, phase, refrigerant, capacity, (found * 100 / 7))
    }
}
