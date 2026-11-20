package com.salanow.ezannow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.salanow.ezannow.MainActivity
import com.salanow.ezannow.R
import com.salanow.ezannow.data.preferences.UserSettings
import com.salanow.ezannow.data.preferences.UserSettingsRepository
import com.salanow.ezannow.domain.calculator.HijriCalendar
import com.salanow.ezannow.domain.calculator.PrayerTimeCalculator
import com.salanow.ezannow.domain.model.PrayerTimesDay
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EzanNowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = UserSettingsRepository(context).currentSettings()
                updateWidgetsWithSettings(context, appWidgetManager, appWidgetIds, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        suspend fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, EzanNowWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val settings = UserSettingsRepository(context).currentSettings()
            updateWidgetsWithSettings(context, manager, ids, settings)
        }

        fun updateWidgetsWithSettings(
            context: Context,
            manager: AppWidgetManager,
            widgetIds: IntArray,
            settings: UserSettings
        ) {
            if (widgetIds.isEmpty()) return
            val nowMs = System.currentTimeMillis()
            val today = SimpleDate.today(settings.location.timeZoneId)
            val yesterday = today.plusDays(-1)
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
            val yesterdayTimes = PrayerTimeCalculator.calculateDay(
                location = settings.location,
                date = yesterday,
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
            val hijri = HijriCalendar.gregorianToHijri(today, settings.hijriDayOffset)

            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
            views.setTextViewText(R.id.widget_city, settings.location.name)
            views.setTextViewText(R.id.widget_date, "${today.formatShortTr()} • ${hijri.formatTr()}")
            views.setTextViewText(R.id.widget_next_label, nextInfo.nextPrayer.turkishName)
            views.setTextViewText(
                R.id.widget_next_time,
                PrayerTimesDay.formatMillis(nextInfo.nextPrayerTimeMillis, settings.location.timeZoneId)
            )

            val cellIds = mapOf(
                PrayerType.IMSAK to (R.id.label_imsak to R.id.time_imsak),
                PrayerType.GUNES to (R.id.label_gunes to R.id.time_gunes),
                PrayerType.OGLE to (R.id.label_ogle to R.id.time_ogle),
                PrayerType.IKINDI to (R.id.label_ikindi to R.id.time_ikindi),
                PrayerType.AKSAM to (R.id.label_aksam to R.id.time_aksam),
                PrayerType.YATSI to (R.id.label_yatsi to R.id.time_yatsi)
            )

            val gold = Color.parseColor("#F5C451")
            val white = Color.WHITE
            val mutedWhite = Color.parseColor("#B3FFFFFF")

            for (prayer in PrayerType.entries) {
                val (labelId, timeId) = cellIds.getValue(prayer)
                val isNext = prayer == nextInfo.nextPrayer
                views.setTextViewText(timeId, todayTimes.formattedTime(prayer))
                views.setTextColor(labelId, if (isNext) gold else mutedWhite)
                views.setTextColor(timeId, if (isNext) gold else white)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPi = PendingIntent.getActivity(
                context,
                5001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launchPi)

            for (id in widgetIds) {
                manager.updateAppWidget(id, views)
            }
        }
    }
}
