package com.salanow.ezannow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salanow.ezannow.data.location.DeviceLocationHelper
import com.salanow.ezannow.data.location.GpsDetectionResult
import com.salanow.ezannow.data.preferences.DhikrItem
import com.salanow.ezannow.data.preferences.UserSettings
import com.salanow.ezannow.data.preferences.UserSettingsRepository
import com.salanow.ezannow.domain.calculator.HijriCalendar
import com.salanow.ezannow.domain.calculator.HijriDate
import com.salanow.ezannow.domain.calculator.PrayerTimeCalculator
import com.salanow.ezannow.domain.calculator.QiblaCalculator
import com.salanow.ezannow.domain.calculator.ReligiousDayItem
import com.salanow.ezannow.domain.model.AsrMadhab
import com.salanow.ezannow.domain.model.CalculationMethod
import com.salanow.ezannow.domain.model.CityLocation
import com.salanow.ezannow.domain.model.HighLatitudeRule
import com.salanow.ezannow.domain.model.KerahatStatus
import com.salanow.ezannow.domain.model.NextPrayerInfo
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerAdjustments
import com.salanow.ezannow.domain.model.PrayerTimesDay
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.QadaPrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import com.salanow.ezannow.domain.model.ThemeMode
import com.salanow.ezannow.domain.testmode.SimulatedNotificationEvent
import com.salanow.ezannow.domain.testmode.SimulationSpeed
import com.salanow.ezannow.domain.testmode.TestModeSimulationHelper
import com.salanow.ezannow.domain.testmode.TestModeState
import com.salanow.ezannow.notification.GentleTonePlayer
import com.salanow.ezannow.notification.PrayerAlarmScheduler
import com.salanow.ezannow.notification.PrayerNotificationHelper
import com.salanow.ezannow.widget.EzanNowWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: UserSettings = UserSettings(),
    val todayDate: SimpleDate = SimpleDate.today("Europe/Istanbul"),
    val selectedDate: SimpleDate = SimpleDate.today("Europe/Istanbul"),
    val todayPrayerTimes: PrayerTimesDay = PrayerTimeCalculator.calculateDay(
        UserSettings().location,
        SimpleDate.today("Europe/Istanbul")
    ),
    val selectedDayPrayerTimes: PrayerTimesDay = todayPrayerTimes,
    val nextPrayerInfo: NextPrayerInfo = NextPrayerInfo(
        currentPrayer = PrayerType.IMSAK,
        nextPrayer = PrayerType.GUNES,
        nextPrayerTimeMillis = 0L,
        previousPrayerTimeMillis = 0L,
        remainingMillis = 0L,
        progress = 0f
    ),
    val kerahatStatus: KerahatStatus = KerahatStatus.None,
    val hijriDate: HijriDate = HijriCalendar.gregorianToHijri(SimpleDate.today("Europe/Istanbul")),
    val sevenDayStrip: List<SimpleDate> = emptyList(),
    val timetableMonthDate: SimpleDate = SimpleDate.today("Europe/Istanbul").let {
        SimpleDate(it.year, it.month, 1)
    },
    val monthPrayerTimes: List<PrayerTimesDay> = emptyList(),
    val qiblaBearing: Double = QiblaCalculator.bearingDegrees(41.0082, 28.9784),
    val qiblaDistanceKm: Double = QiblaCalculator.distanceKm(41.0082, 28.9784),
    val religiousDays: List<ReligiousDayItem> = emptyList(),
    val isDetectingLocation: Boolean = false,
    val statusMessage: String? = null,
    val testMode: TestModeState = TestModeState()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = UserSettingsRepository(appContext)

    private val tickerMillis = MutableStateFlow(System.currentTimeMillis())
    private val selectedDateFlow = MutableStateFlow<SimpleDate?>(null)
    private val timetableMonthFlow = MutableStateFlow<SimpleDate?>(null)
    private val isDetectingFlow = MutableStateFlow(false)
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val testModeFlow = MutableStateFlow(TestModeState())
    private val triggeredSimKeys = mutableSetOf<String>()

    private val monthTimesFlow = combine(
        repository.settingsFlow.map {
            listOf(
                it.location,
                it.calculationMethod,
                it.madhab,
                it.highLatitudeRule,
                it.adjustments
            )
        }.distinctUntilChanged(),
        timetableMonthFlow
    ) { calcKey, customMonth ->
        @Suppress("UNCHECKED_CAST")
        val loc = calcKey[0] as CityLocation
        val method = calcKey[1] as CalculationMethod
        val madhab = calcKey[2] as AsrMadhab
        val highLat = calcKey[3] as HighLatitudeRule
        val adj = calcKey[4] as PrayerAdjustments
        val today = SimpleDate.today(loc.timeZoneId)
        val monthAnchor = customMonth ?: SimpleDate(today.year, today.month, 1)
        monthAnchor to PrayerTimeCalculator.calculateMonth(
            location = loc,
            year = monthAnchor.year,
            month = monthAnchor.month,
            method = method,
            madhab = madhab,
            highLatRule = highLat,
            userAdjustments = adj
        )
    }

    private val religiousDaysFlow = repository.settingsFlow
        .map { it.location.timeZoneId to it.hijriDayOffset }
        .distinctUntilChanged()
        .map { (tzId, offset) ->
            HijriCalendar.upcomingReligiousDays(SimpleDate.today(tzId), offset)
        }

    private val metaFlow = combine(
        isDetectingFlow,
        statusMessageFlow,
        monthTimesFlow,
        religiousDaysFlow
    ) { detecting, msg, monthPair, religious ->
        MetaBundle(detecting, msg, monthPair.first, monthPair.second, religious)
    }

    private data class MetaBundle(
        val detecting: Boolean,
        val statusMsg: String?,
        val monthAnchor: SimpleDate,
        val monthTimes: List<PrayerTimesDay>,
        val religiousDays: List<ReligiousDayItem>
    )

    val uiState: StateFlow<MainUiState> = combine(
        repository.settingsFlow,
        tickerMillis,
        selectedDateFlow,
        metaFlow,
        testModeFlow
    ) { settings, nowMs, customSelectedDate, meta, testMode ->
        val today = SimpleDate.today(settings.location.timeZoneId)
        val selectedDate = if (testMode.enabled) today else (customSelectedDate ?: today)
        val effectiveNowMs = if (testMode.enabled) {
            TestModeSimulationHelper.simulatedEpochMillis(
                today,
                settings.location.timeZoneId,
                testMode.simulatedSecondsOfDay
            )
        } else {
            nowMs
        }

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

        val selectedDayTimes = if (selectedDate == today) {
            todayTimes
        } else {
            PrayerTimeCalculator.calculateDay(
                location = settings.location,
                date = selectedDate,
                method = settings.calculationMethod,
                madhab = settings.madhab,
                highLatRule = settings.highLatitudeRule,
                userAdjustments = settings.adjustments
            )
        }

        val nextPrayer = PrayerTimeCalculator.computeNextPrayer(
            today = todayTimes,
            tomorrow = tomorrowTimes,
            yesterday = yesterdayTimes,
            nowMillis = effectiveNowMs
        )

        val kerahat = if (settings.showKerahatBanner) {
            PrayerTimeCalculator.computeKerahatStatus(todayTimes, effectiveNowMs)
        } else {
            KerahatStatus.None
        }

        val strip = (-1..5).map { today.plusDays(it) }
        val hijri = HijriCalendar.gregorianToHijri(selectedDate, settings.hijriDayOffset)

        MainUiState(
            settings = settings,
            todayDate = today,
            selectedDate = selectedDate,
            todayPrayerTimes = todayTimes,
            selectedDayPrayerTimes = selectedDayTimes,
            nextPrayerInfo = nextPrayer,
            kerahatStatus = kerahat,
            hijriDate = hijri,
            sevenDayStrip = strip,
            timetableMonthDate = meta.monthAnchor,
            monthPrayerTimes = meta.monthTimes,
            qiblaBearing = QiblaCalculator.bearingDegrees(
                settings.location.latitude,
                settings.location.longitude
            ),
            qiblaDistanceKm = QiblaCalculator.distanceKm(
                settings.location.latitude,
                settings.location.longitude
            ),
            religiousDays = meta.religiousDays,
            isDetectingLocation = meta.detecting,
            statusMessage = meta.statusMsg,
            testMode = testMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            while (true) {
                tickerMillis.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
        viewModelScope.launch {
            while (true) {
                val cur = testModeFlow.value
                if (cur.enabled && cur.isPlaying) {
                    advanceSimulationStep(cur)
                }
                delay(100L)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            PrayerAlarmScheduler.scheduleNextAlarm(appContext)
            PrayerNotificationHelper.refreshPersistentStatusNotification(appContext)
            EzanNowWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun refreshOnForeground() {
        tickerMillis.value = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            PrayerAlarmScheduler.scheduleNextAlarm(appContext)
            PrayerNotificationHelper.refreshPersistentStatusNotification(appContext)
            EzanNowWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    fun showStatusMessage(message: String) {
        statusMessageFlow.value = message
    }

    fun selectDate(date: SimpleDate) {
        selectedDateFlow.value = if (date == uiState.value.todayDate) null else date
    }

    fun jumpToToday() {
        selectedDateFlow.value = null
    }

    fun selectCity(city: CityLocation) {
        viewModelScope.launch {
            repository.setLocation(city)
            selectedDateFlow.value = null
            statusMessageFlow.value = "Konum seçildi: ${city.name}"
            syncBackgroundServices()
        }
    }

    fun detectGpsLocation() {
        if (isDetectingFlow.value) return
        viewModelScope.launch {
            isDetectingFlow.value = true
            try {
                when (val result = DeviceLocationHelper.detectCurrentLocation(appContext)) {
                    is GpsDetectionResult.Success -> {
                        repository.setLocation(result.location)
                        selectedDateFlow.value = null
                        statusMessageFlow.value = "GPS konumu güncellendi: ${result.location.name}"
                        syncBackgroundServices()
                    }
                    GpsDetectionResult.PermissionMissing -> {
                        statusMessageFlow.value = "Konum izni gerekli."
                    }
                    GpsDetectionResult.ProviderDisabled -> {
                        statusMessageFlow.value = "Lütfen cihaz konum (GPS) servisini açın."
                    }
                    GpsDetectionResult.Unavailable -> {
                        statusMessageFlow.value = "Konum alınamadı, lütfen şehir seçin."
                    }
                }
            } finally {
                isDetectingFlow.value = false
            }
        }
    }

    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            repository.setCalculationMethod(method)
            syncBackgroundServices()
        }
    }

    fun setMadhab(madhab: AsrMadhab) {
        viewModelScope.launch {
            repository.setMadhab(madhab)
            syncBackgroundServices()
        }
    }

    fun setHighLatitudeRule(rule: HighLatitudeRule) {
        viewModelScope.launch {
            repository.setHighLatitudeRule(rule)
            syncBackgroundServices()
        }
    }

    fun setAdjustments(adjustments: PrayerAdjustments) {
        viewModelScope.launch {
            repository.setAdjustments(adjustments)
            statusMessageFlow.value = "Dakika düzeltmeleri kaydedildi."
            syncBackgroundServices()
        }
    }

    fun setHijriDayOffset(offset: Int) {
        viewModelScope.launch {
            repository.setHijriDayOffset(offset)
            syncBackgroundServices()
        }
    }

    fun setShowKerahatBanner(show: Boolean) {
        viewModelScope.launch {
            repository.setShowKerahatBanner(show)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun togglePrayerNotification(type: PrayerType) {
        viewModelScope.launch {
            val current = uiState.value.settings.enabledNotifications[type] ?: true
            val next = !current
            repository.setPrayerNotificationEnabled(type, next)
            statusMessageFlow.value = if (next) {
                "${type.turkishName} bildirimi açıldı."
            } else {
                "${type.turkishName} bildirimi kapatıldı."
            }
            syncBackgroundServices()
        }
    }

    fun setNotificationLeadMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setNotificationLeadMinutes(minutes)
            syncBackgroundServices()
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationSoundEnabled(enabled)
            val updated = repository.currentSettings()
            PrayerNotificationHelper.ensureChannel(appContext, updated.notificationSoundPreset)
            if (!enabled) {
                GentleTonePlayer.stop()
            }
            syncBackgroundServices()
        }
    }

    fun setPersistentStatusNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPersistentStatusNotificationEnabled(enabled)
            syncBackgroundServices()
        }
    }

    fun selectNotificationSoundPreset(preset: NotificationSoundPreset) {
        viewModelScope.launch {
            repository.setNotificationSoundPreset(preset)
            PrayerNotificationHelper.ensureChannel(appContext, preset)
            val currentSettings = repository.currentSettings()
            val customUri = currentSettings.customNotificationSoundUri
            if (preset == NotificationSoundPreset.SILENT) {
                GentleTonePlayer.stop()
            } else {
                GentleTonePlayer.play(
                    context = appContext,
                    preset = preset,
                    customUriString = customUri,
                    durationSeconds = currentSettings.notificationSoundDurationSeconds
                )
            }
            syncBackgroundServices()
        }
    }

    fun setNotificationSoundDurationSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.setNotificationSoundDurationSeconds(seconds)
            syncBackgroundServices()
        }
    }

    fun setCustomNotificationSound(uri: String, displayName: String) {
        viewModelScope.launch {
            repository.setCustomNotificationSound(uri, displayName)
            PrayerNotificationHelper.ensureChannel(appContext, NotificationSoundPreset.CUSTOM_URI)
            val durationSec = repository.currentSettings().notificationSoundDurationSeconds
            GentleTonePlayer.play(
                context = appContext,
                preset = NotificationSoundPreset.CUSTOM_URI,
                customUriString = uri,
                durationSeconds = durationSec
            )
            statusMessageFlow.value = "Özel bildirim sesi seçildi: $displayName"
            syncBackgroundServices()
        }
    }

    fun previewGentleTone() {
        val settings = uiState.value.settings
        if (settings.notificationSoundPreset == NotificationSoundPreset.SILENT) {
            statusMessageFlow.value = "Bildirim sesi şu an sessiz modda."
        } else {
            GentleTonePlayer.play(
                context = appContext,
                preset = settings.notificationSoundPreset,
                customUriString = settings.customNotificationSoundUri,
                durationSeconds = settings.notificationSoundDurationSeconds
            )
        }
    }

    fun sendTestNotification() {
        val settings = uiState.value.settings
        val sent = PrayerNotificationHelper.showTestNotification(
            context = appContext,
            cityName = settings.location.name,
            soundPreset = settings.notificationSoundPreset,
            customSoundUri = settings.customNotificationSoundUri,
            soundDurationSeconds = settings.notificationSoundDurationSeconds
        )
        statusMessageFlow.value = if (sent) {
            "Test bildirimi gönderildi."
        } else {
            "Bildirim göndermek için bildirim izni verin."
        }
    }

    fun scheduleBackgroundSwipeTestAlarm() {
        val settings = uiState.value.settings
        PrayerAlarmScheduler.scheduleDelayedTestAlarm(appContext, settings, delaySeconds = 10)
        statusMessageFlow.value =
            "10 sn sonra test alarmı kuruldu. Uygulamayı kapatıp (kaydırarak) test edebilirsiniz."
    }

    fun prevTimetableMonth() {
        val current = uiState.value.timetableMonthDate
        timetableMonthFlow.value = current.plusMonths(-1)
    }

    fun nextTimetableMonth() {
        val current = uiState.value.timetableMonthDate
        timetableMonthFlow.value = current.plusMonths(1)
    }

    fun resetTimetableMonth() {
        timetableMonthFlow.value = null
    }

    fun selectDhikr(id: String) {
        viewModelScope.launch {
            repository.saveDhikrState(uiState.value.settings.dhikrList, id, 0L)
        }
    }

    fun incrementDhikr() {
        viewModelScope.launch {
            val s = uiState.value.settings
            val updated = s.dhikrList.map { item ->
                if (item.id == s.selectedDhikrId) item.copy(count = item.count + 1) else item
            }
            repository.saveDhikrState(updated, s.selectedDhikrId, 1L)
        }
    }

    fun decrementDhikr() {
        viewModelScope.launch {
            val s = uiState.value.settings
            var changed = false
            val updated = s.dhikrList.map { item ->
                if (item.id == s.selectedDhikrId && item.count > 0) {
                    changed = true
                    item.copy(count = item.count - 1)
                } else {
                    item
                }
            }
            if (changed) {
                repository.saveDhikrState(updated, s.selectedDhikrId, 0L)
            }
        }
    }

    fun resetCurrentDhikr() {
        viewModelScope.launch {
            val s = uiState.value.settings
            val updated = s.dhikrList.map { item ->
                if (item.id == s.selectedDhikrId) item.copy(count = 0) else item
            }
            repository.saveDhikrState(updated, s.selectedDhikrId, 0L)
        }
    }

    fun addCustomDhikr(title: String, subtitle: String, target: Int) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            val s = uiState.value.settings
            val id = "custom_${System.currentTimeMillis()}"
            val newItem = DhikrItem(
                id = id,
                title = cleanTitle,
                subtitle = subtitle.trim(),
                count = 0,
                target = target.coerceIn(1, 10_000),
                isPreset = false
            )
            repository.saveDhikrState(s.dhikrList + newItem, id, 0L)
            statusMessageFlow.value = "\"$cleanTitle\" eklendi."
        }
    }

    fun deleteCustomDhikr(id: String) {
        viewModelScope.launch {
            val s = uiState.value.settings
            val targetItem = s.dhikrList.firstOrNull { it.id == id } ?: return@launch
            if (targetItem.isPreset) return@launch
            val remaining = s.dhikrList.filterNot { it.id == id }
            if (remaining.isEmpty()) return@launch
            val nextSelected = if (s.selectedDhikrId == id) remaining.first().id else s.selectedDhikrId
            repository.saveDhikrState(remaining, nextSelected, 0L)
        }
    }

    fun toggleDhikrHaptic() {
        viewModelScope.launch {
            repository.setDhikrHapticEnabled(!uiState.value.settings.dhikrHapticEnabled)
        }
    }

    fun adjustQada(type: QadaPrayerType, delta: Int) {
        viewModelScope.launch {
            val current = uiState.value.settings.qadaCounts[type] ?: 0
            repository.updateQadaCount(type, (current + delta).coerceAtLeast(0))
        }
    }

    fun resetAllQada() {
        viewModelScope.launch {
            repository.resetAllQada()
            statusMessageFlow.value = "Kaza sayacı sıfırlandı."
        }
    }

    fun setTestModeEnabled(enabled: Boolean) {
        if (enabled) {
            selectedDateFlow.value = null
            triggeredSimKeys.clear()
            testModeFlow.value = testModeFlow.value.copy(
                enabled = true,
                isPlaying = false,
                simulatedSecondsOfDay = 0.0,
                triggeredEvents = emptyList()
            )
        } else {
            GentleTonePlayer.stop()
            triggeredSimKeys.clear()
            testModeFlow.value = testModeFlow.value.copy(
                enabled = false,
                isPlaying = false
            )
        }
    }

    fun toggleTestModePlayPause() {
        val cur = testModeFlow.value
        if (!cur.enabled) return
        testModeFlow.value = cur.copy(isPlaying = !cur.isPlaying)
    }

    fun setTestModeSpeed(speed: SimulationSpeed) {
        testModeFlow.value = testModeFlow.value.copy(speed = speed)
    }

    fun setTestModeMinutes(minutes: Float) {
        val newSec = (minutes.toDouble() * 60.0).coerceIn(0.0, 86399.0)
        val curState = uiState.value
        val today = curState.todayDate
        val settings = curState.settings
        val newSimMs = TestModeSimulationHelper.simulatedEpochMillis(
            today,
            settings.location.timeZoneId,
            newSec
        )
        val leadMs = settings.notificationLeadMinutes * 60_000L
        PrayerType.entries.forEach { prayer ->
            val triggerMs = curState.todayPrayerTimes.millisOf(prayer) - leadMs
            if (triggerMs > newSimMs) {
                triggeredSimKeys.remove(prayer.name)
            }
        }
        testModeFlow.value = testModeFlow.value.copy(simulatedSecondsOfDay = newSec)
    }

    fun resetTestModeClock() {
        GentleTonePlayer.stop()
        PrayerNotificationHelper.cancelAllNotifications(appContext)
        triggeredSimKeys.clear()
        testModeFlow.value = testModeFlow.value.copy(
            isPlaying = false,
            simulatedSecondsOfDay = 0.0,
            triggeredEvents = emptyList()
        )
    }

    fun jumpBeforeNextSimulatedPrayer() {
        val curState = uiState.value
        val cur = testModeFlow.value
        val settings = curState.settings
        val targetSec = TestModeSimulationHelper.secondsBeforeNextTrigger(
            currentSecondsOfDay = cur.simulatedSecondsOfDay,
            todayTimes = curState.todayPrayerTimes,
            enabledNotifications = settings.enabledNotifications,
            leadMinutes = settings.notificationLeadMinutes,
            leadBeforeSeconds = 60.0
        )
        val newSimMs = TestModeSimulationHelper.simulatedEpochMillis(
            curState.todayDate,
            settings.location.timeZoneId,
            targetSec
        )
        val leadMs = settings.notificationLeadMinutes * 60_000L
        PrayerType.entries.forEach { prayer ->
            val triggerMs = curState.todayPrayerTimes.millisOf(prayer) - leadMs
            if (triggerMs > newSimMs) {
                triggeredSimKeys.remove(prayer.name)
            }
        }
        testModeFlow.value = cur.copy(
            enabled = true,
            isPlaying = true,
            simulatedSecondsOfDay = targetSec
        )
    }

    private fun advanceSimulationStep(cur: TestModeState) {
        val currentUi = uiState.value
        val settings = currentUi.settings
        val today = currentUi.todayDate
        val todayTimes = currentUi.todayPrayerTimes
        val tzId = settings.location.timeZoneId

        val prevSec = cur.simulatedSecondsOfDay
        val deltaSec = cur.speed.minutesPerSecond * 6.0
        val rawNextSec = prevSec + deltaSec

        val prevSimMs = TestModeSimulationHelper.simulatedEpochMillis(today, tzId, prevSec)
        val stepEndSec = rawNextSec.coerceAtMost(86400.0)
        val stepEndMs = TestModeSimulationHelper.simulatedEpochMillis(today, tzId, stepEndSec)

        val crossed = TestModeSimulationHelper.findCrossedPrayerTriggers(
            prevSimMillis = prevSimMs,
            newSimMillis = stepEndMs,
            todayTimes = todayTimes,
            enabledNotifications = settings.enabledNotifications,
            leadMinutes = settings.notificationLeadMinutes,
            alreadyTriggeredKeys = triggeredSimKeys
        )

        val updatedEvents = cur.triggeredEvents.toMutableList()
        for (prayer in crossed) {
            triggeredSimKeys.add(prayer.name)
            PrayerNotificationHelper.showPrayerNotification(
                context = appContext,
                prayerType = prayer,
                prayerTimeFormatted = todayTimes.formattedTime(prayer),
                cityName = settings.location.name,
                leadMinutes = settings.notificationLeadMinutes,
                isSimulation = true,
                soundPreset = settings.notificationSoundPreset,
                customSoundUri = settings.customNotificationSoundUri,
                soundDurationSeconds = settings.notificationSoundDurationSeconds
            )
            val eventClock = cur.copy(simulatedSecondsOfDay = stepEndSec.coerceAtMost(86399.0)).formattedClock()
            updatedEvents.add(
                0,
                SimulatedNotificationEvent(
                    prayer = prayer,
                    prayerTimeFormatted = todayTimes.formattedTime(prayer),
                    leadMinutes = settings.notificationLeadMinutes,
                    triggeredAtSimTime = eventClock
                )
            )
        }

        val (finalSec, finalPlaying) = if (rawNextSec >= 86400.0) {
            PrayerNotificationHelper.cancelAllNotifications(appContext)
            if (cur.loopDay) {
                triggeredSimKeys.clear()
                (rawNextSec - 86400.0) to true
            } else {
                86399.0 to false
            }
        } else {
            rawNextSec to true
        }

        testModeFlow.value = cur.copy(
            simulatedSecondsOfDay = finalSec,
            isPlaying = finalPlaying,
            triggeredEvents = updatedEvents.take(30)
        )
    }

    private suspend fun syncBackgroundServices() {
        val latest = repository.currentSettings()
        PrayerAlarmScheduler.scheduleNextAlarm(appContext, latest)
        PrayerNotificationHelper.refreshPersistentStatusNotification(appContext, latest)
        EzanNowWidgetProvider.updateAllWidgets(appContext)
    }

    override fun onCleared() {
        GentleTonePlayer.stop()
        super.onCleared()
    }
}
