package com.salanow.ezannow.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.salanow.ezannow.MainActivity
import com.salanow.ezannow.R
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerType
import java.util.Calendar
import java.util.TimeZone

object PrayerNotificationHelper {

    private const val LEGACY_CHANNEL_V1 = "ezannow_prayer_alerts"
    private const val LEGACY_CHANNEL_V2 = "ezannow_prayer_alerts_v2"
    private const val CHANNEL_V3_PREFIX = "ezannow_alert_v3_"
    const val CHANNEL_ID_SILENT = "ezannow_prayer_silent_v2"

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

            val activeSoundChannelId = channelIdForPreset(activePreset)
            NotificationSoundPreset.entries.forEach { candidate ->
                val candidateId = channelIdForPreset(candidate)
                if (candidateId != CHANNEL_ID_SILENT &&
                    candidateId != activeSoundChannelId &&
                    nm.getNotificationChannel(candidateId) != null
                ) {
                    nm.deleteNotificationChannel(candidateId)
                }
            }

            if (activeSoundChannelId != CHANNEL_ID_SILENT &&
                nm.getNotificationChannel(activeSoundChannelId) == null
            ) {
                val uri = soundUriForPreset(context, activePreset)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val channel = NotificationChannel(
                    activeSoundChannelId,
                    "${context.getString(R.string.notification_channel_name)} (${activePreset.turkishName})",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    if (uri != null) {
                        setSound(uri, attrs)
                    }
                    enableVibration(true)
                    enableLights(true)
                }
                nm.createNotificationChannel(channel)
            }

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
            NotificationManagerCompat.from(context).cancelAll()
        }
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
        soundDurationSeconds: Int = GentleTonePlayer.DEFAULT_DURATION_SECONDS
    ) {
        if (!hasNotificationPermission(context)) return
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

        // Play all notification sounds via GentleTonePlayer on a silent channel with configured duration
        val useDirectPlayer = true
        val channelId = CHANNEL_ID_SILENT

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setTimeoutAfter(millisUntilMidnight())
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (useDirectPlayer || soundPreset == NotificationSoundPreset.SILENT) {
            builder.setSilent(true)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            soundUriForPreset(context, soundPreset)?.let { builder.setSound(it) }
        }

        val notifId = (if (isSimulation) 3000 else 2000) + prayerType.ordinal
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }

        if (useDirectPlayer && soundPreset != NotificationSoundPreset.SILENT) {
            GentleTonePlayer.play(
                context = context,
                preset = soundPreset,
                customUriString = customSoundUri,
                durationSeconds = soundDurationSeconds
            )
        }
    }

    fun showTestNotification(
        context: Context,
        cityName: String,
        soundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1,
        customSoundUri: String = "",
        soundDurationSeconds: Int = GentleTonePlayer.DEFAULT_DURATION_SECONDS
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SILENT)
            .setSmallIcon(R.drawable.ic_notification_mosque)
            .setContentTitle("EzanNow! Test Bildirimi")
            .setContentText("$cityName için namaz vakti bildirimleri aktif (${soundPreset.turkishName}, $soundDurationSeconds sn).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTimeoutAfter(millisUntilMidnight())
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val posted = runCatching {
            NotificationManagerCompat.from(context).notify(2999, notification)
            true
        }.getOrDefault(false)

        if (posted && soundPreset != NotificationSoundPreset.SILENT) {
            GentleTonePlayer.play(
                context = context,
                preset = soundPreset,
                customUriString = customSoundUri,
                durationSeconds = soundDurationSeconds
            )
        }
        return posted
    }
}
