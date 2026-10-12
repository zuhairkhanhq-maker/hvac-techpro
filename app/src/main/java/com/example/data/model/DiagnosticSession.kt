package com.example.data.model

data class DiagnosticMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val quickActions: List<String> = emptyList()
)

enum class MessageSender {
    TECHNICIAN,
    SENIOR_TECH_AI
}

data class QuickDiagnosticScenario(
    val title: String,
    val subtitle: String,
    val initialPrompt: String,
    val category: String
)

object DiagnosticScenarios {
    val fieldScenarios = listOf(
        QuickDiagnosticScenario(
            title = "Compressor Hum & Tripping Breaker",
            subtitle = "Motor buzzing on startup, pulls LRA, clicks off",
            initialPrompt = "My compressor is humming loudly on startup for 3 seconds then tripping the breaker / internal overload. Fan motor runs fine. How do I test the capacitor vs locked rotor?",
            category = "Electrical"
        ),
        QuickDiagnosticScenario(
            title = "Reversing Valve Stuck (Cooling vs Heating)",
            subtitle = "Heat pump blowing warm air in AC mode or cold in heat",
            initialPrompt = "Heat pump is blowing 85°F air in cooling mode. Reversing valve solenoid has 24VAC across O/B terminal. How do I check if the slide is stuck vs bad solenoid coil?",
            category = "Refrigeration"
        ),
        QuickDiagnosticScenario(
            title = "Low Suction + High Superheat + Normal Subcool",
            subtitle = "Classic TXV failure vs system undercharge dilemma",
            initialPrompt = "R-410A system has 80 psig suction (32°F superheat, target 12°F) and 340 psig liquid pressure (11°F subcooling). Is this an undercharge or restricted TXV?",
            category = "Refrigeration"
        ),
        QuickDiagnosticScenario(
            title = "Gas Furnace Ignites Then Drops Flame in 4s",
            subtitle = "Trial for ignition aborts after 3-5 seconds",
            initialPrompt = "Furnace burners ignite with a clean blue flame, but shut off completely after 4 seconds. Repeats 3 times then goes into lockout. How do I test the flame sensor microamps?",
            category = "Heating"
        ),
        QuickDiagnosticScenario(
            title = "Frozen Evaporator Coil Troubleshooting",
            subtitle = "Coil solid block of ice from bottom to top",
            initialPrompt = "Evaporator coil is encased in a thick block of ice. What is my exact checklist to identify whether it's low refrigerant, bad blower motor, or clogged filter?",
            category = "Airflow"
        )
    )
}
