package com.salanow.ezannow

import com.salanow.ezannow.domain.calculator.QiblaCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {

    @Test
    fun istanbulBearingAndDistanceAreAccurate() {
        val bearing = QiblaCalculator.bearingDegrees(41.0082, 28.9784)
        val distance = QiblaCalculator.distanceKm(41.0082, 28.9784)

        assertEquals(151.6, bearing, 1.5)
        assertEquals(2400.0, distance, 60.0)
    }

    @Test
    fun ankaraAndBerlinBearingsAreAccurate() {
        val ankaraBearing = QiblaCalculator.bearingDegrees(39.9334, 32.8597)
        val berlinBearing = QiblaCalculator.bearingDegrees(52.5200, 13.4050)

        assertEquals(160.2, ankaraBearing, 1.5)
        assertEquals(136.6, berlinBearing, 1.5)
    }

    @Test
    fun shortestDeltaWrapsAcrossZeroAnd360Properly() {
        assertEquals(10f, QiblaCalculator.shortestDeltaDegrees(355f, 5f), 0.001f)
        assertEquals(-10f, QiblaCalculator.shortestDeltaDegrees(5f, 355f), 0.001f)
        assertTrue(QiblaCalculator.isAligned(150f, 154f))
        assertTrue(QiblaCalculator.isAligned(2f, 358f))
        assertFalse(QiblaCalculator.isAligned(140f, 152f))
    }
}
