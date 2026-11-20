package com.salanow.ezannow.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.salanow.ezannow.MainActivity
import com.salanow.ezannow.R
import com.salanow.ezannow.data.preferences.UserSettings
import com.salanow.ezannow.data.preferences.UserSettingsRepository
import com.salanow.ezannow.domain.calculator.PrayerTimeCalculator
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerTimesDay
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import java.util.Calendar
import java.util.TimeZone

object PrayerNotificationHelper {

    private const val LEGACY_CHANNEL_V1 = "ezannow_prayer_alerts"
    private const val LEGACY_CHANNEL_V2 = "ezannow_prayer_alerts_v2"
    private const val CHANNEL_V3_PREFIX = "ezannow_alert_v3_"
    const val CHANNEL_ID_SILENT = "ezannow_prayer_silent_v2"
    const val CHANNEL_ID_PERSISTENT_STATUS = "ezannow_persistent_status_v1"
    const val PERSISTENT_STATUS_NOTIFICATION_ID = 1999

    fun channelIdForPreset(preset: NotificationSoundPreset): String {
        return when (preset) {
            NotificationSoundPreset.SILENT,
            NotificationSoundPreset.CUSTOM_URI -> CHANNEL_ID_SILENT
            else -> "$CHANNEL_V3_PREFIX${preset.id}"
        }
    }

    fun soundUriForPreset(context: Context, preset: NotificationSoundPreset): Uri? {
        val rawRes = GentleTonePlayer.rawResForPreset(preset)
        if (rawRes != null) {
            return Uri.parse("android.resource://${context.packageName}/$rawRes")
        }
        if (preset == NotificationSoundPreset.SYSTEM_DEFAULT) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        return null
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(
        context: Context,
        activePreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            listOf(LEGACY_CHANNEL_V1, LEGACY_CHANNEL_V2).forEach { legacy ->
                if (nm.getNotificationChannel(legacy) != null) {
                    nm.deleteNotificationChannel(legacy)
                }
            }

            nm.notificationChannels
                .filter { it.id.startsWith(CHANNEL_V3_PREFIX) }
                .forEach { stale -> nm.deleteNotificationChannel(stale.id) }

            if (nm.getNotificationChannel(CHANNEL_ID_SILENT) == null) {
                val silentChannel = NotificationChannel(
                    CHANNEL_ID_SILENT,
                    context.getString(R.string.notification_channel_silent_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    setSound(null, null)
                    enableVibration(true)
                    enableLights(true)
                }
                nm.createNotificationChannel(silentChannel)
            }

            if (nm.getNotificationChannel(CHANNEL_ID_PERSISTENT_STATUS) == null) {
                val statusChannel = NotificationChannel(
                    CHANNEL_ID_PERSISTENT_STATUS,
                    context.getString(R.string.notification_channel_status_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notification_channel_status_desc)
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(false)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(statusChannel)
            }
        }
    }

    fun millisUntilMidnight(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - nowMillis).coerceAtLeast(1_000L)
    }

    fun cancelAllNotifications(context: Context) {
        runCatching {
            val nm = NotificationManagerCompat.from(context)
            PrayerType.entries.forEach { prayer ->
                nm.cancel(notificationIdFor(prayer, isSimulation = false))
                nm.cancel(notificationIdFor(prayer, isSimulation = true))
            }
            nm.cancel(TEST_NOTIFICATION_ID)
            nm.cancel(CUSTOM_TEST_NOTIFICATION_ID)
        }
    }

    fun cancelPersistentStatusNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(PERSISTENT_STATUS_NOTIFICATION_ID)
        }
    }

    suspend fun refreshPersistentStatusNotification(context: Context) {
        val settings = UserSettingsRepository(context).currentSettings()
        refreshPersistentStatusNotification(context, settings)
    }

    fun refreshPersistentStatusNotification(context: Context, settings: UserSettings) {
        if (!settings.persistentStatusNotificationEnabled || !hasNotificationPermission(context)) {
            cancelPersistentStatusNotification(context)
            return
        }
        ensureChannel(context, settings.notificationSoundPreset)

        val nowMs = System.currentTimeMillis()
        val today = SimpleDate.today(settings.location.timeZoneId)
        val todayTimes = PrayerTimeCalculator.calculateDay(
            location = settings.location,
            date = today,
            method = settings.calculationMethod,
            madhab = settings.madhab,
            highLatRule = settings.highLatitudeRule,
            userAdjustments = settings.adjustments
        )
        val yesterdayTimes = PrayerTimeCalculator.calculateDay(
            location = settings.location,
            date = today.plusDays(-1),
            method = settings.calculationMethod,
            madhab = settings.madhab,
            highLatRule = settings.highLatitudeRule,
            userAdjustments = settings.adjustments
        )
        val tomorrowTimes = PrayerTimeCalculator.calculateDay(
            location = settings.location,
            date = today.plusDays(1),
            method = settings.calculationMethod,
            madhab = settings.madhab,
            highLatRule = settings.highLatitudeRule,
            userAdjustments = settings.adjustments
        )

        val nextInfo = PrayerTimeCalculator.computeNextPrayer(
            today = todayTimes,
            tomorrow = tomorrowTimes,
            yesterday = yesterdayTimes,
            nowMillis = nowMs
        )

        val nextTimeStr = PrayerTimesDay.formatMillis(
            nextInfo.nextPrayerTimeMillis,
            settings.location.timeZoneId
        )
        val title = "Sıradaki: ${nextInfo.nextPrayer.turkishName} • $nextTimeStr"
        val summaryLine = "${settings.location.name} • Şimdiki: ${nextInfo.currentPrayer.turkishName}"
        val expandedText = buildString {
            append(summaryLine)
            append('\n')
            append("İmsak ${todayTimes.formattedTime(PrayerType.IMSAK)} • ")
            append("Güneş ${todayTimes.formattedTime(PrayerType.GUNES)} • ")
            append("Öğle ${todayTimes.formattedTime(PrayerType.OGLE)}")
            append('\n')
            append("İkindi ${todayTimes.formattedTime(PrayerType.IKINDI)} • ")
            append("Akşam ${todayTimes.formattedTime(PrayerType.AKSAM)} • ")
            append("Yatsı ${todayTimes.formattedTime(PrayerType.YATSI)}")
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            1999,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PERSISTENT_STATUS)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setWhen(nextInfo.nextPrayerTimeMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPi)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(PERSISTENT_STATUS_NOTIFICATION_ID, notification)
        }
    }

    const val CUSTOM_TEST_NOTIFICATION_ID = 2998
    const val TEST_NOTIFICATION_ID = 2999

    fun notificationIdFor(prayerType: PrayerType, isSimulation: Boolean = false): Int {
        return (if (isSimulation) 3000 else 2000) + prayerType.ordinal
    }

    fun buildPrayerNotification(
        context: Context,
        prayerType: PrayerType,
        prayerTimeFormatted: String,
        cityName: String,
        leadMinutes: Int,
        isSimulation: Boolean = false,
        soundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1
    ): Notification {
        ensureChannel(context, soundPreset)

        val baseTitle = if (leadMinutes > 0) {
            "${prayerType.turkishName} Vaktine $leadMinutes dk Kaldı"
        } else {
            "${prayerType.turkishName} Vakti Girdi"
        }
        val title = if (isSimulation) "[TEST] $baseTitle" else baseTitle

        val body = if (leadMinutes > 0) {
            "$cityName • Vakit saati: $prayerTimeFormatted"
        } else {
            "$cityName • $prayerTimeFormatted — Hayırlı vakitler."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal + if (isSimulation) 300 else 100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_SILENT)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setTimeoutAfter(millisUntilMidnight())
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun showPrayerNotification(
        context: Context,
        prayerType: PrayerType,
        prayerTimeFormatted: String,
        cityName: String,
        leadMinutes: Int,
        isSimulation: Boolean = false,
        soundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1,
        customSoundUri: String = "",
        soundDurationSeconds: Int = GentleTonePlayer.DEFAULT_DURATION_SECONDS,
        onPlaybackFinished: (() -> Unit)? = null
    ) {
        if (!hasNotificationPermission(context)) {
            onPlaybackFinished?.invoke()
            return
        }

        val notification = buildPrayerNotification(
            context = context,
            prayerType = prayerType,
            prayerTimeFormatted = prayerTimeFormatted,
            cityName = cityName,
            leadMinutes = leadMinutes,
            isSimulation = isSimulation,
            soundPreset = soundPreset
        )

        val notifId = notificationIdFor(prayerType, isSimulation)
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }

        if (soundPreset != NotificationSoundPreset.SILENT) {
            GentleTonePlayer.play(
                context = context,
                preset = soundPreset,
                customUriString = customSoundUri,
                durationSeconds = soundDurationSeconds,
                useAlarmStream = !isSimulation,
                onFinished = onPlaybackFinished
            )
        } else {
            onPlaybackFinished?.invoke()
        }
    }

    fun buildTestNotification(
        context: Context,
        cityName: String,
        soundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1,
        soundDurationSeconds: Int = GentleTonePlayer.DEFAULT_DURATION_SECONDS
    ): Notification {
        ensureChannel(context, soundPreset)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            999,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_SILENT)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle("EzanNow! Test Bildirimi")
            .setContentText("$cityName için namaz vakti bildirimleri aktif (${soundPreset.turkishName}, $soundDurationSeconds sn).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setTimeoutAfter(millisUntilMidnight())
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun showTestNotification(
        context: Context,
        cityName: String,
        soundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1,
        customSoundUri: String = "",
        soundDurationSeconds: Int = GentleTonePlayer.DEFAULT_DURATION_SECONDS,
        onPlaybackFinished: (() -> Unit)? = null
    ): Boolean {
        if (!hasNotificationPermission(context)) {
            onPlaybackFinished?.invoke()
            return false
        }

        val notification = buildTestNotification(
            context = context,
            cityName = cityName,
            soundPreset = soundPreset,
            soundDurationSeconds = soundDurationSeconds
        )

        val posted = runCatching {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)

        if (posted && soundPreset != NotificationSoundPreset.SILENT) {
            GentleTonePlayer.play(
                context = context,
                preset = soundPreset,
                customUriString = customSoundUri,
                durationSeconds = soundDurationSeconds,
                useAlarmStream = false,
                onFinished = onPlaybackFinished
            )
        } else {
            onPlaybackFinished?.invoke()
        }
        return posted
    }

    fun buildCustomTestTimeNotification(
        context: Context,
        timeFormatted: String,
        cityName: String,
        soundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1
    ): Notification {
        ensureChannel(context, soundPreset)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            998,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = buildString {
            if (cityName.isNotBlank()) append("$cityName • ")
            if (timeFormatted.isNotBlank()) append("$timeFormatted — ")
            append("Geliştirici özel test vakti bildirimi.")
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_SILENT)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle("Özel Test Vakti Girdi")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setTimeoutAfter(millisUntilMidnight())
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun showCustomTestTimeNotification(
        context: Context,
        timeFormatted: String,
        cityName: String,
        soundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1,
        customSoundUri: String = "",
        soundDurationSeconds: Int = GentleTonePlayer.DEFAULT_DURATION_SECONDS,
        onPlaybackFinished: (() -> Unit)? = null
    ): Boolean {
        if (!hasNotificationPermission(context)) {
            onPlaybackFinished?.invoke()
            return false
        }

        val notification = buildCustomTestTimeNotification(
            context = context,
            timeFormatted = timeFormatted,
            cityName = cityName,
            soundPreset = soundPreset
        )

        val posted = runCatching {
            NotificationManagerCompat.from(context).notify(CUSTOM_TEST_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)

        if (posted && soundPreset != NotificationSoundPreset.SILENT) {
            GentleTonePlayer.play(
                context = context,
                preset = soundPreset,
                customUriString = customSoundUri,
                durationSeconds = soundDurationSeconds,
                useAlarmStream = true,
                onFinished = onPlaybackFinished
            )
        } else {
            onPlaybackFinished?.invoke()
        }
        return posted
    }
}
