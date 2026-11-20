package com.salanow.ezannow.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.salanow.ezannow.data.preferences.UserSettings
import com.salanow.ezannow.data.preferences.UserSettingsRepository
import com.salanow.ezannow.domain.calculator.PrayerTimeCalculator
import com.salanow.ezannow.domain.model.PrayerTimesDay
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import java.util.Calendar
import java.util.TimeZone

object PrayerAlarmScheduler {

    private const val REQ_PRAYER_ALARM = 4101
    private const val REQ_MIDNIGHT_REFRESH = 4102

    const val EXTRA_KIND = "extra_kind"
    const val KIND_PRAYER = "prayer"
    const val KIND_REFRESH = "refresh"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    const val EXTRA_CITY_NAME = "extra_city_name"
    const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
    const val EXTRA_SOUND_PRESET = "extra_sound_preset"
    const val EXTRA_CUSTOM_SOUND_URI = "extra_custom_sound_uri"
    const val EXTRA_SOUND_DURATION_SEC = "extra_sound_duration_sec"

    suspend fun scheduleNextAlarm(context: Context) {
        val settings = UserSettingsRepository(context).currentSettings()
        scheduleNextAlarm(context, settings)
    }

    fun scheduleNextAlarm(context: Context, settings: UserSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nowMs = System.currentTimeMillis()
        val today = SimpleDate.today(settings.location.timeZoneId)
        val tomorrow = today.plusDays(1)

        val todayTimes = PrayerTimeCalculator.calculateDay(
            location = settings.location,
            date = today,
            method = settings.calculationMethod,
            madhab = settings.madhab,
            highLatRule = settings.highLatitudeRule,
            userAdjustments = settings.adjustments
        )
        val tomorrowTimes = PrayerTimeCalculator.calculateDay(
            location = settings.location,
            date = tomorrow,
            method = settings.calculationMethod,
            madhab = settings.madhab,
            highLatRule = settings.highLatitudeRule,
            userAdjustments = settings.adjustments
        )

        val leadMs = settings.notificationLeadMinutes * 60_000L
        val candidateList = buildList {
            for (dayTimes in listOf(todayTimes, tomorrowTimes)) {
                for (prayer in PrayerType.entries) {
                    if (settings.enabledNotifications[prayer] == true) {
                        val prayerMs = dayTimes.millisOf(prayer)
                        val triggerMs = prayerMs - leadMs
                        if (triggerMs > nowMs + 1_500L) {
                            add(Triple(triggerMs, prayer, dayTimes.formattedTime(prayer)))
                        }
                    }
                }
            }
        }.minByOrNull { it.first }

        if (candidateList != null) {
            val (triggerMs, prayer, formattedTime) = candidateList
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra(EXTRA_KIND, KIND_PRAYER)
                putExtra(EXTRA_PRAYER_NAME, prayer.name)
                putExtra(EXTRA_PRAYER_TIME, formattedTime)
                putExtra(EXTRA_CITY_NAME, settings.location.name)
                putExtra(EXTRA_LEAD_MINUTES, settings.notificationLeadMinutes)
                putExtra(EXTRA_SOUND_PRESET, settings.notificationSoundPreset.name)
                putExtra(EXTRA_CUSTOM_SOUND_URI, settings.customNotificationSoundUri)
                putExtra(EXTRA_SOUND_DURATION_SEC, settings.notificationSoundDurationSeconds)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQ_PRAYER_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarmSafe(alarmManager, triggerMs, pi)
        } else {
            val cancelIntent = Intent(context, PrayerAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                REQ_PRAYER_ALARM,
                cancelIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
            }
        }

        // Schedule daily 24:00 (00:00) refresh so notifications clear and alarms/widget roll over
        val tz = TimeZone.getTimeZone(settings.location.timeZoneId)
        val midnightCal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (midnightCal.timeInMillis <= nowMs + 2_000L) {
            midnightCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        val refreshIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(EXTRA_KIND, KIND_REFRESH)
        }
        val refreshPi = PendingIntent.getBroadcast(
            context,
            REQ_MIDNIGHT_REFRESH,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarmSafe(alarmManager, midnightCal.timeInMillis, refreshPi)
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun setAlarmSafe(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        runCatching {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }.onFailure {
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }
}
