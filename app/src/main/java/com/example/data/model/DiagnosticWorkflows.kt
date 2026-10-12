package com.example.data.model

data class DiagnosticWorkflow(val key: String, val title: String, val safety: String, val steps: List<String>)

object DiagnosticWorkflows {
    val all = listOf(
        DiagnosticWorkflow("no_cooling", "No cooling", "Disconnect power before opening panels. Never bypass a safety control.", listOf("Confirm thermostat demand and operating mode", "Record indoor return and supply temperatures", "Confirm indoor and outdoor airflow", "Record suction/liquid pressures and line temperatures", "Compare measurements with the exact equipment charging procedure", "Record the evidence supporting the technician's finding")),
        DiagnosticWorkflow("no_heating", "No heating", "If gas is smelled, leave the area and contact the fuel supplier/emergency service. Do not operate switches.", listOf("Confirm thermostat demand", "Record the displayed fault pattern before reset", "Verify power and safety-chain state using the manufacturer procedure", "Record temperature rise or heat-pump measurements", "Confirm the cause before replacing a component")),
        DiagnosticWorkflow("poor_airflow", "Poor airflow", "Stop the system if icing or unsafe overheating is present.", listOf("Inspect filter and return restrictions", "Confirm blower settings and wheel condition", "Record return and supply static pressure", "Record temperature split", "Inspect coil and duct restrictions", "Compare with rated airflow data")),
        DiagnosticWorkflow("compressor_start", "Compressor will not start", "Capacitors can retain hazardous charge. Use approved procedures and PPE.", listOf("Verify call and correct supply voltage", "Record run/start capacitor measured value and rating", "Inspect contactor voltage drop and connections", "Record winding/resistance checks only with power isolated", "Check protection state and manufacturer diagnostic procedure", "Do not repeatedly reset protection")),
        DiagnosticWorkflow("short_cycle", "Short cycling", "Do not bypass pressure, temperature or combustion safeties.", listOf("Record cycle duration and fault indication", "Confirm thermostat and control inputs", "Check airflow and temperature conditions", "Record pressure/control events", "Confirm whether the stop is control-driven or safety-driven")),
        DiagnosticWorkflow("water_leak", "Water leakage", "Protect electrical equipment from water before inspection.", listOf("Identify the actual source", "Inspect drain, trap and slope", "Check coil icing and airflow", "Inspect pump and overflow safeties", "Confirm drainage after repair"))
    )
}
