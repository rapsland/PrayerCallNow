package com.salanow.ezannow.domain.calculator

import com.salanow.ezannow.domain.model.AsrMadhab
import com.salanow.ezannow.domain.model.CalculationMethod
import com.salanow.ezannow.domain.model.CityLocation
import com.salanow.ezannow.domain.model.HighLatitudeRule
import com.salanow.ezannow.domain.model.KerahatStatus
import com.salanow.ezannow.domain.model.NextPrayerInfo
import com.salanow.ezannow.domain.model.PrayerAdjustments
import com.salanow.ezannow.domain.model.PrayerTimesDay
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

object PrayerTimeCalculator {

    private const val SUNRISE_SUNSET_ANGLE = 0.833
    private const val MILLIS_PER_HOUR = 3_600_000.0
    private const val MILLIS_PER_MINUTE = 60_000L

    private data class SolarPosition(
        val declinationRad: Double,
        val equationOfTimeHours: Double
    )

    fun calculateDay(
        location: CityLocation,
        date: SimpleDate,
        method: CalculationMethod = CalculationMethod.DIYANET,
        madhab: AsrMadhab = AsrMadhab.STANDARD,
        highLatRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
        userAdjustments: PrayerAdjustments = PrayerAdjustments()
    ): PrayerTimesDay {
        val jd0 = julianDay(date.year, date.month, date.day)
        val lon = location.longitude
        val latRad = Math.toRadians(location.latitude)

        // Initial UT noon guess for the given longitude
        val initialNoonDayFrac = 0.5 - lon / 360.0
        val solarNoon = solarPosition(jd0 + initialNoonDayFrac)
        val utTransit = 12.0 - lon / 15.0 - solarNoon.equationOfTimeHours

        // Iterative refinement per prayer event
        val sunriseApprox = utTransit - 6.0
        val sunsetApprox = utTransit + 6.0
        val fajrApprox = utTransit - 7.0
        val asrApprox = utTransit + 3.5
        val ishaApprox = utTransit + 7.0

        val transitRefined = computeTransitUt(jd0, utTransit / 24.0, lon)

        val sunriseSolar = solarPosition(jd0 + sunriseApprox / 24.0)
        val sunriseHa = hourAngleHours(latRad, sunriseSolar.declinationRad, -Math.toRadians(SUNRISE_SUNSET_ANGLE))
        val sunriseUt = if (!sunriseHa.isNaN()) {
            computeTransitUt(jd0, sunriseApprox / 24.0, lon) - sunriseHa
        } else {
            transitRefined - 6.0
        }

        val sunsetSolar = solarPosition(jd0 + sunsetApprox / 24.0)
        val sunsetHa = hourAngleHours(latRad, sunsetSolar.declinationRad, -Math.toRadians(SUNRISE_SUNSET_ANGLE))
        val sunsetUt = if (!sunsetHa.isNaN()) {
            computeTransitUt(jd0, sunsetApprox / 24.0, lon) + sunsetHa
        } else {
            transitRefined + 6.0
        }

        val nightDurationHours = (24.0 - (sunsetUt - sunriseUt)).coerceIn(1.0, 23.0)

        // Fajr (İmsak)
        val fajrSolar = solarPosition(jd0 + fajrApprox / 24.0)
        val fajrHa = hourAngleHours(latRad, fajrSolar.declinationRad, -Math.toRadians(method.fajrAngle))
        val rawFajrUt = if (!fajrHa.isNaN()) {
            computeTransitUt(jd0, fajrApprox / 24.0, lon) - fajrHa
        } else {
            Double.NaN
        }
        val fajrDiffMax = nightPortion(highLatRule, method.fajrAngle, nightDurationHours)
        val fajrUt = if (rawFajrUt.isNaN() || (sunriseUt - rawFajrUt) > fajrDiffMax) {
            sunriseUt - fajrDiffMax
        } else {
            rawFajrUt
        }

        // Asr (İkindi)
        val asrSolar = solarPosition(jd0 + asrApprox / 24.0)
        val asrAltRad = atan(1.0 / (madhab.shadowFactor + tan(abs(latRad - asrSolar.declinationRad))))
        val asrHa = hourAngleHours(latRad, asrSolar.declinationRad, asrAltRad)
        val asrUt = if (!asrHa.isNaN()) {
            computeTransitUt(jd0, asrApprox / 24.0, lon) + asrHa
        } else {
            transitRefined + 3.0
        }

        // Isha (Yatsı)
        val ishaUt = if (method.ishaIntervalMinutes != null) {
            sunsetUt + method.ishaIntervalMinutes / 60.0
        } else {
            val ishaSolar = solarPosition(jd0 + ishaApprox / 24.0)
            val ishaHa = hourAngleHours(latRad, ishaSolar.declinationRad, -Math.toRadians(method.ishaAngle))
            val rawIshaUt = if (!ishaHa.isNaN()) {
                computeTransitUt(jd0, ishaApprox / 24.0, lon) + ishaHa
            } else {
                Double.NaN
            }
            val ishaDiffMax = nightPortion(highLatRule, method.ishaAngle, nightDurationHours)
            if (rawIshaUt.isNaN() || (rawIshaUt - sunsetUt) > ishaDiffMax) {
                sunsetUt + ishaDiffMax
            } else {
                rawIshaUt
            }
        }

        val utcMidnightMillis = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun toEpochMillis(utHours: Double, type: PrayerType): Long {
            val totalAdjMinutes = method.methodOffsets.forPrayer(type) + userAdjustments.forPrayer(type)
            val rawMillis = utcMidnightMillis + (utHours * MILLIS_PER_HOUR).roundToLong() + totalAdjMinutes * MILLIS_PER_MINUTE
            // Round to nearest minute so displayed HH:mm matches exact boundary triggers
            return ((rawMillis + 30_000L) / MILLIS_PER_MINUTE) * MILLIS_PER_MINUTE
        }

        val imsakMs = toEpochMillis(fajrUt, PrayerType.IMSAK)
        val gunesMs = maxOf(imsakMs + MILLIS_PER_MINUTE, toEpochMillis(sunriseUt, PrayerType.GUNES))
        val ogleMs = maxOf(gunesMs + MILLIS_PER_MINUTE, toEpochMillis(transitRefined, PrayerType.OGLE))
        val ikindiMs = maxOf(ogleMs + MILLIS_PER_MINUTE, toEpochMillis(asrUt, PrayerType.IKINDI))
        val aksamMs = maxOf(ikindiMs + MILLIS_PER_MINUTE, toEpochMillis(sunsetUt, PrayerType.AKSAM))
        val yatsiMs = maxOf(aksamMs + MILLIS_PER_MINUTE, toEpochMillis(ishaUt, PrayerType.YATSI))

        return PrayerTimesDay(
            date = date,
            timesMillis = mapOf(
                PrayerType.IMSAK to imsakMs,
                PrayerType.GUNES to gunesMs,
                PrayerType.OGLE to ogleMs,
                PrayerType.IKINDI to ikindiMs,
                PrayerType.AKSAM to aksamMs,
                PrayerType.YATSI to yatsiMs
            ),
            timeZoneId = location.timeZoneId
        )
    }

    fun calculateMonth(
        location: CityLocation,
        year: Int,
        month: Int,
        method: CalculationMethod = CalculationMethod.DIYANET,
        madhab: AsrMadhab = AsrMadhab.STANDARD,
        highLatRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
        userAdjustments: PrayerAdjustments = PrayerAdjustments()
    ): List<PrayerTimesDay> {
        val firstDay = SimpleDate(year, month, 1)
        val count = firstDay.daysInMonth()
        return (1..count).map { day ->
            calculateDay(
                location = location,
                date = SimpleDate(year, month, day),
                method = method,
                madhab = madhab,
                highLatRule = highLatRule,
                userAdjustments = userAdjustments
            )
        }
    }

    fun computeNextPrayer(
        today: PrayerTimesDay,
        tomorrow: PrayerTimesDay,
        yesterday: PrayerTimesDay,
        nowMillis: Long
    ): NextPrayerInfo {
        val ordered = PrayerType.entries
        for (i in ordered.indices) {
            val candidate = ordered[i]
            val candidateMs = today.millisOf(candidate)
            if (nowMillis < candidateMs) {
                val prevPrayer: PrayerType
                val prevMs: Long
                if (i == 0) {
                    prevPrayer = PrayerType.YATSI
                    prevMs = yesterday.millisOf(PrayerType.YATSI)
                } else {
                    prevPrayer = ordered[i - 1]
                    prevMs = today.millisOf(prevPrayer)
                }
                val totalSpan = (candidateMs - prevMs).coerceAtLeast(1L)
                val elapsed = (nowMillis - prevMs).coerceIn(0L, totalSpan)
                return NextPrayerInfo(
                    currentPrayer = prevPrayer,
                    nextPrayer = candidate,
                    nextPrayerTimeMillis = candidateMs,
                    previousPrayerTimeMillis = prevMs,
                    remainingMillis = (candidateMs - nowMillis).coerceAtLeast(0L),
                    progress = (elapsed.toDouble() / totalSpan.toDouble()).toFloat().coerceIn(0f, 1f)
                )
            }
        }

        // After Yatsı -> next is tomorrow's İmsak
        val yatsiMs = today.millisOf(PrayerType.YATSI)
        val nextImsakMs = tomorrow.millisOf(PrayerType.IMSAK)
        val totalSpan = (nextImsakMs - yatsiMs).coerceAtLeast(1L)
        val elapsed = (nowMillis - yatsiMs).coerceIn(0L, totalSpan)
        return NextPrayerInfo(
            currentPrayer = PrayerType.YATSI,
            nextPrayer = PrayerType.IMSAK,
            nextPrayerTimeMillis = nextImsakMs,
            previousPrayerTimeMillis = yatsiMs,
            remainingMillis = (nextImsakMs - nowMillis).coerceAtLeast(0L),
            progress = (elapsed.toDouble() / totalSpan.toDouble()).toFloat().coerceIn(0f, 1f)
        )
    }

    fun computeKerahatStatus(today: PrayerTimesDay, nowMillis: Long): KerahatStatus {
        val kerahatDurationMs = 45 * MILLIS_PER_MINUTE
        val upcomingLeadMs = 15 * MILLIS_PER_MINUTE

        val gunes = today.millisOf(PrayerType.GUNES)
        val ogle = today.millisOf(PrayerType.OGLE)
        val aksam = today.millisOf(PrayerType.AKSAM)

        val windows = listOf(
            Triple("İşrak / Güneş Doğuş Kerahati", gunes, gunes + kerahatDurationMs),
            Triple("İstiva (Öğle Öncesi) Kerahati", ogle - kerahatDurationMs, ogle),
            Triple("İsfirar (Akşam Öncesi) Kerahati", aksam - kerahatDurationMs, aksam)
        )

        for ((label, start, end) in windows) {
            if (nowMillis in start until end) {
                val remMin = ((end - nowMillis + 59_999L) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
                return KerahatStatus.Active(label = label, remainingMinutes = remMin)
            }
        }

        for ((label, start, _) in windows) {
            if (nowMillis in (start - upcomingLeadMs) until start) {
                val untilMin = ((start - nowMillis + 59_999L) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
                return KerahatStatus.Upcoming(label = label, minutesUntilStart = untilMin)
            }
        }

        return KerahatStatus.None
    }

    private fun computeTransitUt(jd0: Double, dayFraction: Double, longitude: Double): Double {
        val pos = solarPosition(jd0 + dayFraction)
        return 12.0 - longitude / 15.0 - pos.equationOfTimeHours
    }

    private fun hourAngleHours(latRad: Double, declRad: Double, altitudeRad: Double): Double {
        val denominator = cos(latRad) * cos(declRad)
        if (abs(denominator) < 1e-10) return Double.NaN
        val cosHa = (sin(altitudeRad) - sin(latRad) * sin(declRad)) / denominator
        if (cosHa < -1.0 || cosHa > 1.0) return Double.NaN
        return Math.toDegrees(acos(cosHa)) / 15.0
    }

    private fun nightPortion(
        rule: HighLatitudeRule,
        angleDegrees: Double,
        nightHours: Double
    ): Double = when (rule) {
        HighLatitudeRule.MIDDLE_OF_NIGHT -> nightHours * 0.5
        HighLatitudeRule.SEVENTH_OF_NIGHT -> nightHours / 7.0
        HighLatitudeRule.TWILIGHT_ANGLE -> nightHours * (angleDegrees / 60.0)
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun solarPosition(jd: Double): SolarPosition {
        val d = jd - 2451545.0
        val g = Math.toRadians(fixAngle(357.529 + 0.98560028 * d))
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = Math.toRadians(fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2.0 * g)))
        val e = Math.toRadians(23.439 - 0.00000036 * d)

        val raHours = fixHours(Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0)
        val declRad = asin(sin(e) * sin(l))
        var eqT = (q / 15.0) - raHours
        while (eqT > 12.0) eqT -= 24.0
        while (eqT < -12.0) eqT += 24.0
        return SolarPosition(declinationRad = declRad, equationOfTimeHours = eqT)
    }

    private fun fixAngle(deg: Double): Double {
        var res = deg % 360.0
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHours(hours: Double): Double {
        var res = hours % 24.0
        if (res < 0) res += 24.0
        return res
    }
}
