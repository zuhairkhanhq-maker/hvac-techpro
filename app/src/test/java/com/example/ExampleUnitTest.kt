package com.example
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test
class ExampleUnitTest {
    @Test fun ptBoundsAndAnchors() {
        RefrigerantData.allRefrigerants.forEach { ref ->
            ref.samplePoints.forEach { (p,t) -> assertEquals(t,ref.satTempFromPsig(p.toDouble()),0.1); assertEquals(p.toDouble(),ref.psigFromTempF(t),0.1) }
            listOf(Double.NaN, Double.POSITIVE_INFINITY, -100.0).forEach { value -> assertThrows(IllegalArgumentException::class.java) { ref.satTempFromPsig(value) } }
        }
    }
    @Test fun chargeArithmeticNeverCertifies() {
        val r=A2LCalculator.calculate(A2LRefrigerants.R454B,350.0,8.0,6.5,10.0,0.6)
        assertEquals(2800.0,r.roomVolumeCuFt,0.01); assertEquals(6.875,r.totalSystemChargeLbs,0.001)
        assertEquals(ComplianceStatus.NOT_EVALUATED,r.status)
        assertTrue(r.generateCertificate("","","","").contains("NOT A COMPLIANCE CERTIFICATE"))
    }
    @Test fun invalidChargeInputRejected() {
        listOf(-1.0,0.0,Double.NaN,Double.POSITIVE_INFINITY).forEach { value -> assertThrows(IllegalArgumentException::class.java) { A2LCalculator.calculate(A2LRefrigerants.R32,value,8.0,6.0,0.0,0.0) } }
        assertThrows(IllegalArgumentException::class.java) { A2LCalculator.calculate(A2LRefrigerants.R32,100.0,8.0,-1.0,0.0,0.0) }
    }
    @Test fun tierMatrix() {
        UserTier.entries.forEach { assertTrue(it.canAccessCalculators && it.canAccessPTCharts && it.canAccessBrainDump) }
        listOf(UserTier.NEW_USER,UserTier.REGULAR_USER).forEach { assertFalse(it.canAccessSeniorTechAI || it.canAccessA2LCompliance || it.canAccessDataPlateScanner) }
        assertTrue(UserTier.PLUS_USER.canAccessSeniorTechAI && UserTier.PLUS_USER.canAccessDataPlateScanner && UserTier.PLUS_USER.canCreateReports)
        assertTrue(UserTier.PRO_USER.canAccessDataPlateScanner)
    }
    @Test fun nameplateParserExtractsOnlyPresentFields() {
        val parsed=NameplateParser.parse("CARRIER MODEL 24ABC636A003 SERIAL 1923E12345 VOLTS 208/230 PH 1 REFRIGERANT R-410A CAPACITY 36000 BTUH")
        assertEquals("Carrier",parsed.manufacturer); assertEquals("24ABC636A003",parsed.model); assertEquals("1923E12345",parsed.serial)
        assertEquals("208/230",parsed.voltage); assertEquals("1",parsed.phase); assertEquals("R-410A",parsed.refrigerant); assertTrue(parsed.confidence>=70)
        assertEquals(NameplateFields(),NameplateParser.parse(""))
    }
}
