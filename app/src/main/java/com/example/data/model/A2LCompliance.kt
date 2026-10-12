package com.example.data.model

import java.util.Locale

data class A2LRefrigerantSpec(val name: String, val gwp: Int)
object A2LRefrigerants {
    val R454B = A2LRefrigerantSpec("R-454B", 466)
    val R32 = A2LRefrigerantSpec("R-32", 675)
    val list = listOf(R454B, R32)
}
enum class ComplianceStatus { NOT_EVALUATED }
data class A2LComplianceResult(
    val refrigerant: A2LRefrigerantSpec, val roomAreaSqFt: Double, val ceilingHeightFt: Double,
    val roomVolumeCuFt: Double, val factoryChargeLbs: Double, val lineSetLengthFt: Double,
    val additionalLineOz: Double, val totalSystemChargeLbs: Double,
    val status: ComplianceStatus = ComplianceStatus.NOT_EVALUATED
) {
    fun generateCertificate(technicianName: String, companyName: String, equipmentModel: String, siteAddress: String) = buildString {
        appendLine("A2L CHARGE WORKSHEET — NOT A COMPLIANCE CERTIFICATE")
        appendLine("Technician: ${technicianName.ifBlank { "Not supplied" }}")
        appendLine("Company: ${companyName.ifBlank { "Not supplied" }}")
        appendLine("Equipment: ${equipmentModel.ifBlank { "Not supplied" }}")
        appendLine("Site: ${siteAddress.ifBlank { "Not supplied" }}")
        appendLine("Refrigerant: ${refrigerant.name}")
        appendLine("Room volume: $roomVolumeCuFt ft³")
        appendLine("Factory charge: $factoryChargeLbs lb")
        appendLine("Additional chargeable line: $lineSetLengthFt ft; additional charge: $additionalLineOz oz")
        appendLine("Estimated total: ${String.format(Locale.US, "%.3f", totalSystemChargeLbs)} lb")
        appendLine("Compliance not evaluated. Check exact installation instructions, minimum area, installation height, releasable charge, mitigation and locally adopted code with the authority having jurisdiction.")
    }
}
object A2LCalculator {
    fun calculate(refrigerant: A2LRefrigerantSpec, areaSqFt: Double, heightFt: Double,
        factoryChargeLbs: Double, lineLengthFt: Double, extraChargeOzPerFt: Double): A2LComplianceResult {
        require(areaSqFt.isFinite() && areaSqFt in 0.1..1000000.0) { "Enter a positive room area." }
        require(heightFt.isFinite() && heightFt in 0.1..1000.0) { "Enter a positive ceiling height." }
        require(factoryChargeLbs.isFinite() && factoryChargeLbs in 0.0..100000.0) { "Factory charge must be nonnegative." }
        require(lineLengthFt.isFinite() && lineLengthFt in 0.0..10000.0) { "Enter additional chargeable line length." }
        require(extraChargeOzPerFt.isFinite() && extraChargeOzPerFt in 0.0..1000.0) { "Enter the manufacturer's charge rate." }
        val extra = lineLengthFt * extraChargeOzPerFt
        return A2LComplianceResult(refrigerant, areaSqFt, heightFt, areaSqFt * heightFt, factoryChargeLbs, lineLengthFt, extra, factoryChargeLbs + extra / 16)
    }
}
