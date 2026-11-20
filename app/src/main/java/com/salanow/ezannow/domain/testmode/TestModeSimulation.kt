package com.salanow.ezannow.domain.testmode

import com.salanow.ezannow.domain.model.PrayerTimesDay
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

enum class SimulationSpeed(
    val label: String,
    val minutesPerSecond: Float
) {
    VERY_SLOW("1 dk/sn", 1f),
    SLOW("5 dk/sn", 5f),
    NORMAL("15 dk/sn", 15f),
    FAST("30 dk/sn", 30f),
    VERY_FAST("60 dk/sn", 60f)
}

data class SimulatedNotificationEvent(
    val prayer: PrayerType,
    val prayerTimeFormatted: String,
    val leadMinutes: Int,
    val triggeredAtSimTime: String
)

data class TestModeState(
    val enabled: Boolean = false,
    val isPlaying: Boolean = false,
    val simulatedSecondsOfDay: Double = 0.0,
    val speed: SimulationSpeed = SimulationSpeed.NORMAL,
    val loopDay: Boolean = true,
    val triggeredEvents: List<SimulatedNotificationEvent> = emptyList()
) {
    fun formattedClock(): String {
        val totalSec = simulatedSecondsOfDay.toInt().coerceIn(0, 86399)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return String.format(Locale.forLanguageTag("tr-TR"), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun minutesOfDayFloat(): Float {
        return (simulatedSecondsOfDay / 60.0).toFloat().coerceIn(0f, 1440f)
    }
}

object TestModeSimulationHelper {

    fun dayStartMillis(date: SimpleDate, timeZoneId: String): Long {
        val tz = TimeZone.getTimeZone(timeZoneId)
        return GregorianCalendar(tz).apply {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun simulatedEpochMillis(
        date: SimpleDate,
        timeZoneId: String,
        simulatedSecondsOfDay: Double
    ): Long {
        val clamped = simulatedSecondsOfDay.coerceIn(0.0, 86400.0)
        return dayStartMillis(date, timeZoneId) + (clamped * 1000.0).toLong()
    }

    fun findCrossedPrayerTriggers(
        prevSimMillis: Long,
        newSimMillis: Long,
        todayTimes: PrayerTimesDay,
        enabledNotifications: Map<PrayerType, Boolean>,
        leadMinutes: Int,
        alreadyTriggeredKeys: Set<String>
    ): List<PrayerType> {
        if (newSimMillis <= prevSimMillis) return emptyList()
        val leadMs = leadMinutes * 60_000L
        return PrayerType.entries.filter { prayer ->
            val enabled = enabledNotifications[prayer] ?: false
            if (!enabled) return@filter false
            if (prayer.name in alreadyTriggeredKeys) return@filter false
            val triggerMs = todayTimes.millisOf(prayer) - leadMs
            triggerMs > prevSimMillis && triggerMs <= newSimMillis
        }
    }

    fun secondsBeforeNextTrigger(
        currentSecondsOfDay: Double,
        todayTimes: PrayerTimesDay,
        enabledNotifications: Map<PrayerType, Boolean>,
        leadMinutes: Int,
        leadBeforeSeconds: Double = 60.0
    ): Double {
        val dayStart = dayStartMillis(todayTimes.date, todayTimes.timeZoneId)
        val leadMs = leadMinutes * 60_000L
        val enabledTriggerSeconds = PrayerType.entries
            .filter { enabledNotifications[it] == true }
            .map { prayer ->
                val triggerMs = todayTimes.millisOf(prayer) - leadMs
                ((triggerMs - dayStart) / 1000.0).coerceIn(0.0, 86399.0)
            }
            .sorted()

        if (enabledTriggerSeconds.isEmpty()) {
            return currentSecondsOfDay.coerceIn(0.0, 86399.0)
        }

        // Pick next trigger strictly ahead of current + 5s buffer, or wrap to first trigger of the day
        val targetTriggerSec = enabledTriggerSeconds.firstOrNull { it > currentSecondsOfDay + 5.0 }
            ?: enabledTriggerSeconds.first()

        return (targetTriggerSec - leadBeforeSeconds).coerceIn(0.0, 86399.0)
    }
}
