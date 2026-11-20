package com.salanow.ezannow.domain.calculator

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaCalculator {

    const val KAABA_LATITUDE = 21.4225241
    const val KAABA_LONGITUDE = 39.8261818
    const val EARTH_RADIUS_KM = 6371.0
    const val ALIGN_THRESHOLD_DEGREES = 5.0f

    fun bearingDegrees(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return normalize360(Math.toDegrees(theta))
    }

    fun distanceKm(latitude: Double, longitude: Double): Double {
        val dLat = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLon = Math.toRadians(KAABA_LONGITUDE - longitude)
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)

        val sinLat = sin(dLat / 2.0)
        val sinLon = sin(dLon / 2.0)
        val a = sinLat * sinLat + cos(phi1) * cos(phi2) * sinLon * sinLon
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_KM * c
    }

    fun shortestDeltaDegrees(currentHeading: Float, targetBearing: Float): Float {
        var diff = (targetBearing - currentHeading) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    fun isAligned(currentHeading: Float, targetBearing: Float): Boolean {
        return abs(shortestDeltaDegrees(currentHeading, targetBearing)) <= ALIGN_THRESHOLD_DEGREES
    }

    fun normalize360(degrees: Double): Double {
        var res = degrees % 360.0
        if (res < 0.0) res += 360.0
        return res
    }

    fun normalize360(degrees: Float): Float {
        var res = degrees % 360f
        if (res < 0f) res += 360f
        return res
    }

    fun cardinalDirectionTr(degrees: Float): String {
        val normalized = normalize360(degrees)
        val dirs = listOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
        val index = (((normalized + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
        return dirs[index]
    }
}
