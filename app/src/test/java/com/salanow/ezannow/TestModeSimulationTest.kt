package com.salanow.ezannow

import com.salanow.ezannow.data.cities.CityCatalog
import com.salanow.ezannow.data.preferences.UserSettings
import com.salanow.ezannow.domain.calculator.PrayerTimeCalculator
import com.salanow.ezannow.domain.model.KerahatStatus
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import com.salanow.ezannow.domain.testmode.TestModeSimulationHelper
import com.salanow.ezannow.domain.testmode.TestModeState
import com.salanow.ezannow.notification.GentleTonePlayer
import com.salanow.ezannow.notification.PrayerNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class TestModeSimulationTest {

    @Test
    fun full24HourSweepTriggersEveryEnabledPrayerAndSkipsDisabled() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 11, 20)
        val day = PrayerTimeCalculator.calculateDay(istanbul, date)
        val enabled = UserSettings.defaultNotificationMap() // GUNES is false, 5 others true

        val triggered = mutableSetOf<String>()
        val firedOrder = mutableListOf<PrayerType>()

        var prevMs = TestModeSimulationHelper.simulatedEpochMillis(date, istanbul.timeZoneId, 0.0)
        // Step through 24h in 5-minute increments
        for (min in 5..1440 step 5) {
            val newMs = TestModeSimulationHelper.simulatedEpochMillis(
                date,
                istanbul.timeZoneId,
                min * 60.0
            )
            val crossed = TestModeSimulationHelper.findCrossedPrayerTriggers(
                prevSimMillis = prevMs,
                newSimMillis = newMs,
                todayTimes = day,
                enabledNotifications = enabled,
                leadMinutes = 0,
                alreadyTriggeredKeys = triggered
            )
            crossed.forEach {
                triggered.add(it.name)
                firedOrder.add(it)
            }
            prevMs = newMs
        }

        assertEquals(
            listOf(
                PrayerType.IMSAK,
                PrayerType.OGLE,
                PrayerType.IKINDI,
                PrayerType.AKSAM,
                PrayerType.YATSI
            ),
            firedOrder
        )
    }

    @Test
    fun leadMinutesShiftsTriggerAndPreventsDuplicateFire() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 11, 20)
        val day = PrayerTimeCalculator.calculateDay(istanbul, date)
        val enabled = mapOf(PrayerType.OGLE to true)
        val leadMinutes = 10

        val ogleMs = day.millisOf(PrayerType.OGLE)
        val prevMs = ogleMs - 11 * 60_000L
        val atLeadMs = ogleMs - 10 * 60_000L
        val afterLeadMs = ogleMs - 9 * 60_000L

        val triggered = mutableSetOf<String>()
        val firstPass = TestModeSimulationHelper.findCrossedPrayerTriggers(
            prevSimMillis = prevMs,
            newSimMillis = atLeadMs,
            todayTimes = day,
            enabledNotifications = enabled,
            leadMinutes = leadMinutes,
            alreadyTriggeredKeys = triggered
        )
        assertEquals(listOf(PrayerType.OGLE), firstPass)
        triggered.add(PrayerType.OGLE.name)

        // Subsequent step should not fire again
        val secondPass = TestModeSimulationHelper.findCrossedPrayerTriggers(
            prevSimMillis = atLeadMs,
            newSimMillis = afterLeadMs,
            todayTimes = day,
            enabledNotifications = enabled,
            leadMinutes = leadMinutes,
            alreadyTriggeredKeys = triggered
        )
        assertTrue(secondPass.isEmpty())
    }

    @Test
    fun secondsBeforeNextTriggerJumpsOneMinuteAhead() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 11, 20)
        val day = PrayerTimeCalculator.calculateDay(istanbul, date)
        val enabled = mapOf(PrayerType.OGLE to true, PrayerType.IKINDI to true)

        val dayStart = TestModeSimulationHelper.dayStartMillis(date, istanbul.timeZoneId)
        val ogleSec = (day.millisOf(PrayerType.OGLE) - dayStart) / 1000.0
        val jumpedSec = TestModeSimulationHelper.secondsBeforeNextTrigger(
            currentSecondsOfDay = 0.0,
            todayTimes = day,
            enabledNotifications = enabled,
            leadMinutes = 0,
            leadBeforeSeconds = 60.0
        )

        assertEquals(ogleSec - 60.0, jumpedSec, 0.5)
    }

    @Test
    fun simulatedClockDrivesPrayerStateAndFormattedClock() {
        val istanbul = CityCatalog.DEFAULT_CITY
        val date = SimpleDate(2026, 11, 20)
        val day = PrayerTimeCalculator.calculateDay(istanbul, date)
        val yesterday = PrayerTimeCalculator.calculateDay(istanbul, date.plusDays(-1))
        val tomorrow = PrayerTimeCalculator.calculateDay(istanbul, date.plusDays(1))

        val simAt1230 = TestModeState(enabled = true, simulatedSecondsOfDay = 12.5 * 3600)
        assertEquals("12:30:00", simAt1230.formattedClock())

        val simMs = TestModeSimulationHelper.simulatedEpochMillis(
            date,
            istanbul.timeZoneId,
            simAt1230.simulatedSecondsOfDay
        )
        val next = PrayerTimeCalculator.computeNextPrayer(day, tomorrow, yesterday, simMs)
        val kerahat = PrayerTimeCalculator.computeKerahatStatus(day, simMs)

        assertEquals(PrayerType.OGLE, next.nextPrayer)
        assertTrue(kerahat is KerahatStatus.Active)
    }

    @Test
    fun millisUntilMidnightComputesExactRemainingMsTo2400() {
        val tz = TimeZone.getTimeZone("Europe/Istanbul")
        val cal = GregorianCalendar(tz).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 20, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val remaining = PrayerNotificationHelper.millisUntilMidnight(
            nowMillis = cal.timeInMillis,
            timeZone = tz
        )
        assertEquals(60_000L, remaining)
    }

    @Test
    fun soundDurationDefaultsToFourSecondsAndSupportsUpToTenSeconds() {
        assertEquals(4, UserSettings.DEFAULT_SOUND_DURATION_SECONDS)
        assertEquals(1, UserSettings.MIN_SOUND_DURATION_SECONDS)
        assertEquals(10, UserSettings.MAX_SOUND_DURATION_SECONDS)
        assertEquals(4, GentleTonePlayer.DEFAULT_DURATION_SECONDS)
        assertEquals(1, GentleTonePlayer.MIN_DURATION_SECONDS)
        assertEquals(10, GentleTonePlayer.MAX_DURATION_SECONDS)
        assertEquals(4, UserSettings().notificationSoundDurationSeconds)
    }

    @Test
    fun persistentStatusNotificationIsEnabledByDefaultWithNonCollidingId() {
        assertTrue(UserSettings().persistentStatusNotificationEnabled)
        val statusId = PrayerNotificationHelper.PERSISTENT_STATUS_NOTIFICATION_ID
        val reservedAlertIds = PrayerType.entries.flatMap { prayer ->
            listOf(
                PrayerNotificationHelper.notificationIdFor(prayer, isSimulation = false),
                PrayerNotificationHelper.notificationIdFor(prayer, isSimulation = true)
            )
        } + PrayerNotificationHelper.TEST_NOTIFICATION_ID
        assertTrue("Status notification ID collided with alert IDs", statusId !in reservedAlertIds)
    }
}
