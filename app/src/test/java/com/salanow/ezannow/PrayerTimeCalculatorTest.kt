package com.salanow.ezannow

import com.salanow.ezannow.data.cities.CityCatalog
import com.salanow.ezannow.domain.calculator.PrayerTimeCalculator
import com.salanow.ezannow.domain.model.AsrMadhab
import com.salanow.ezannow.domain.model.CalculationMethod
import com.salanow.ezannow.domain.model.CityLocation
import com.salanow.ezannow.domain.model.HighLatitudeRule
import com.salanow.ezannow.domain.model.KerahatStatus
import com.salanow.ezannow.domain.model.PrayerAdjustments
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

class PrayerTimeCalculatorTest {

    @Test
    fun istanbulNov20DiyanetMatchesExpectedWindow() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 11, 20)
        val result = PrayerTimeCalculator.calculateDay(
            location = istanbul,
            date = date,
            method = CalculationMethod.DIYANET,
            madhab = AsrMadhab.STANDARD
        )

        assertMinutesNear(6, 21, result.millisOf(PrayerType.IMSAK), istanbul.timeZoneId, 5)
        assertMinutesNear(7, 52, result.millisOf(PrayerType.GUNES), istanbul.timeZoneId, 5)
        assertMinutesNear(12, 55, result.millisOf(PrayerType.OGLE), istanbul.timeZoneId, 5)
        assertMinutesNear(15, 27, result.millisOf(PrayerType.IKINDI), istanbul.timeZoneId, 5)
        assertMinutesNear(17, 49, result.millisOf(PrayerType.AKSAM), istanbul.timeZoneId, 5)
        assertMinutesNear(19, 12, result.millisOf(PrayerType.YATSI), istanbul.timeZoneId, 5)
    }

    @Test
    fun prayersAreStrictlyOrderedAcrossSeasonsAndCities() {
        val cities = listOf(
            CityCatalog.DEFAULT_CITY,
            CityCatalog.ALL_CITIES.first { it.name == "Ankara" },
            CityCatalog.ALL_CITIES.first { it.name == "Berlin" },
            CityCatalog.ALL_CITIES.first { it.name == "Mekke" }
        )
        val dates = listOf(
            SimpleDate(2026, 12, 21),
            SimpleDate(2026, 3, 21),
            SimpleDate(2026, 6, 21),
            SimpleDate(2026, 9, 23)
        )

        for (city in cities) {
            for (date in dates) {
                val day = PrayerTimeCalculator.calculateDay(city, date)
                val times = PrayerType.entries.map { day.millisOf(it) }
                for (i in 0 until times.lastIndex) {
                    assertTrue(
                        "Order failed for ${city.name} on $date at index $i",
                        times[i] < times[i + 1]
                    )
                }
            }
        }
    }

    @Test
    fun hanefiAsrIsStrictlyLaterThanStandardAsr() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 6, 15)
        val standard = PrayerTimeCalculator.calculateDay(
            location = istanbul,
            date = date,
            madhab = AsrMadhab.STANDARD
        )
        val hanafi = PrayerTimeCalculator.calculateDay(
            location = istanbul,
            date = date,
            madhab = AsrMadhab.HANAFI
        )

        assertTrue(
            hanafi.millisOf(PrayerType.IKINDI) > standard.millisOf(PrayerType.IKINDI)
        )
    }

    @Test
    fun userAdjustmentsShiftPrayerTimesByExactMinutes() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 5, 10)
        val base = PrayerTimeCalculator.calculateDay(istanbul, date)
        val adjusted = PrayerTimeCalculator.calculateDay(
            location = istanbul,
            date = date,
            userAdjustments = PrayerAdjustments(imsak = 5, ogle = -4, yatsi = 10)
        )

        assertEquals(5 * 60_000L, adjusted.millisOf(PrayerType.IMSAK) - base.millisOf(PrayerType.IMSAK))
        assertEquals(-4 * 60_000L, adjusted.millisOf(PrayerType.OGLE) - base.millisOf(PrayerType.OGLE))
        assertEquals(10 * 60_000L, adjusted.millisOf(PrayerType.YATSI) - base.millisOf(PrayerType.YATSI))
    }

    @Test
    fun highLatitudeSummerHandlesMidnightTwilightWithoutNaN() {
        val oslo = CityLocation(
            id = "no_oslo",
            name = "Oslo",
            country = "Norveç",
            latitude = 59.9139,
            longitude = 10.7522,
            timeZoneId = "Europe/Oslo"
        )
        val solstice = SimpleDate(2026, 6, 21)

        HighLatitudeRule.entries.forEach { rule ->
            val res = PrayerTimeCalculator.calculateDay(
                location = oslo,
                date = solstice,
                highLatRule = rule
            )
            val imsak = res.millisOf(PrayerType.IMSAK)
            val gunes = res.millisOf(PrayerType.GUNES)
            val yatsi = res.millisOf(PrayerType.YATSI)
            assertTrue(imsak in 1 until gunes)
            assertTrue(yatsi > res.millisOf(PrayerType.AKSAM))
        }
    }

    @Test
    fun kerahatDetectsActiveUpcomingAndClearWindows() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 11, 20)
        val day = PrayerTimeCalculator.calculateDay(istanbul, date)

        val gunes = day.millisOf(PrayerType.GUNES)
        val ogle = day.millisOf(PrayerType.OGLE)
        val aksam = day.millisOf(PrayerType.AKSAM)

        // Inside sunrise kerahat
        assertTrue(PrayerTimeCalculator.computeKerahatStatus(day, gunes + 10 * 60_000L) is KerahatStatus.Active)
        // Upcoming istiva kerahat (50 min before Dhuhr = 5 min before 45-min window)
        assertTrue(PrayerTimeCalculator.computeKerahatStatus(day, ogle - 50 * 60_000L) is KerahatStatus.Upcoming)
        // Inside istiva kerahat
        assertTrue(PrayerTimeCalculator.computeKerahatStatus(day, ogle - 10 * 60_000L) is KerahatStatus.Active)
        // Inside sunset kerahat
        assertTrue(PrayerTimeCalculator.computeKerahatStatus(day, aksam - 15 * 60_000L) is KerahatStatus.Active)
        // Outside any kerahat (20 min after Dhuhr)
        assertEquals(KerahatStatus.None, PrayerTimeCalculator.computeKerahatStatus(day, ogle + 20 * 60_000L))
    }

    private fun assertMinutesNear(
        expectedHour: Int,
        expectedMinute: Int,
        actualMillis: Long,
        timeZoneId: String,
        toleranceMinutes: Int
    ) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
            timeInMillis = actualMillis
        }
        val actualMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val expectedMinutes = expectedHour * 60 + expectedMinute
        val diff = abs(actualMinutes - expectedMinutes)
        assertTrue(
            "Expected ~%02d:%02d, got %02d:%02d (diff=%d min)".format(
                expectedHour,
                expectedMinute,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                diff
            ),
            diff <= toleranceMinutes
        )
    }
}
