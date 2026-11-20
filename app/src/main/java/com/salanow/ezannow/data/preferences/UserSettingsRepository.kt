package com.salanow.ezannow.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salanow.ezannow.data.cities.CityCatalog
import com.salanow.ezannow.domain.model.AsrMadhab
import com.salanow.ezannow.domain.model.CalculationMethod
import com.salanow.ezannow.domain.model.CityLocation
import com.salanow.ezannow.domain.model.HighLatitudeRule
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerAdjustments
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.QadaPrayerType
import com.salanow.ezannow.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ezannow_settings")

data class DhikrItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val count: Int,
    val target: Int,
    val isPreset: Boolean = false
)

data class UserSettings(
    val location: CityLocation = CityCatalog.DEFAULT_CITY,
    val calculationMethod: CalculationMethod = CalculationMethod.DIYANET,
    val madhab: AsrMadhab = AsrMadhab.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_NIGHT,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
    val hijriDayOffset: Int = 0,
    val showKerahatBanner: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val enabledNotifications: Map<PrayerType, Boolean> = defaultNotificationMap(),
    val notificationLeadMinutes: Int = 0,
    val notificationSoundEnabled: Boolean = true,
    val notificationSoundPreset: NotificationSoundPreset = NotificationSoundPreset.MELODI_1,
    val notificationSoundDurationSeconds: Int = DEFAULT_SOUND_DURATION_SECONDS,
    val customNotificationSoundUri: String = "",
    val customNotificationSoundName: String = "",
    val dhikrList: List<DhikrItem> = defaultDhikrPresets(),
    val selectedDhikrId: String = "subhanallah",
    val dhikrHapticEnabled: Boolean = true,
    val totalDhikrCount: Long = 0L,
    val qadaCounts: Map<QadaPrayerType, Int> = QadaPrayerType.entries.associateWith { 0 }
) {
    companion object {
        const val DEFAULT_SOUND_DURATION_SECONDS = 4
        const val MIN_SOUND_DURATION_SECONDS = 1
        const val MAX_SOUND_DURATION_SECONDS = 10

        fun defaultNotificationMap(): Map<PrayerType, Boolean> = mapOf(
            PrayerType.IMSAK to true,
            PrayerType.GUNES to false,
            PrayerType.OGLE to true,
            PrayerType.IKINDI to true,
            PrayerType.AKSAM to true,
            PrayerType.YATSI to true
        )

        fun defaultDhikrPresets(): List<DhikrItem> = listOf(
            DhikrItem("subhanallah", "Sübhanallah", "سُبْحَانَ اللّٰهِ", 0, 33, isPreset = true),
            DhikrItem("elhamdulillah", "Elhamdülillah", "اَلْحَمْدُ لِلّٰهِ", 0, 33, isPreset = true),
            DhikrItem("allahuekber", "Allahu Ekber", "اَللّٰهُ اَكْبَرُ", 0, 33, isPreset = true),
            DhikrItem("lailaheillallah", "La ilahe illallah", "لَا إِلٰهَ إِلَّا اللّٰهُ", 0, 100, isPreset = true),
            DhikrItem("estagfirullah", "Estağfirullah", "أَسْتَغْفِرُ اللّٰهَ", 0, 100, isPreset = true),
            DhikrItem("salavat", "Salavat-ı Şerife", "اللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ", 0, 100, isPreset = true)
        )
    }
}

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val LOC_ID = stringPreferencesKey("loc_id")
        val LOC_NAME = stringPreferencesKey("loc_name")
        val LOC_COUNTRY = stringPreferencesKey("loc_country")
        val LOC_LAT = doublePreferencesKey("loc_lat")
        val LOC_LON = doublePreferencesKey("loc_lon")
        val LOC_TZ = stringPreferencesKey("loc_tz")
        val LOC_IS_GPS = booleanPreferencesKey("loc_is_gps")

        val CALC_METHOD = stringPreferencesKey("calc_method")
        val MADHAB = stringPreferencesKey("madhab")
        val HIGH_LAT = stringPreferencesKey("high_lat")

        val ADJ_IMSAK = intPreferencesKey("adj_imsak")
        val ADJ_GUNES = intPreferencesKey("adj_gunes")
        val ADJ_OGLE = intPreferencesKey("adj_ogle")
        val ADJ_IKINDI = intPreferencesKey("adj_ikindi")
        val ADJ_AKSAM = intPreferencesKey("adj_aksam")
        val ADJ_YATSI = intPreferencesKey("adj_yatsi")

        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val SHOW_KERAHAT = booleanPreferencesKey("show_kerahat")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LEAD_MINUTES = intPreferencesKey("notif_lead_minutes")
        val SOUND_ENABLED = booleanPreferencesKey("notif_sound_enabled")
        val SOUND_PRESET = stringPreferencesKey("notif_sound_preset")
        val SOUND_DURATION_SECONDS = intPreferencesKey("notif_sound_duration_sec")
        val CUSTOM_SOUND_URI = stringPreferencesKey("notif_custom_sound_uri")
        val CUSTOM_SOUND_NAME = stringPreferencesKey("notif_custom_sound_name")

        val NOTIF_IMSAK = booleanPreferencesKey("notif_imsak")
        val NOTIF_GUNES = booleanPreferencesKey("notif_gunes")
        val NOTIF_OGLE = booleanPreferencesKey("notif_ogle")
        val NOTIF_IKINDI = booleanPreferencesKey("notif_ikindi")
        val NOTIF_AKSAM = booleanPreferencesKey("notif_aksam")
        val NOTIF_YATSI = booleanPreferencesKey("notif_yatsi")

        val DHIKR_JSON = stringPreferencesKey("dhikr_json")
        val SELECTED_DHIKR_ID = stringPreferencesKey("selected_dhikr_id")
        val DHIKR_HAPTIC = booleanPreferencesKey("dhikr_haptic")
        val TOTAL_DHIKR = longPreferencesKey("total_dhikr")

        val QADA_SABAH = intPreferencesKey("qada_sabah")
        val QADA_OGLE = intPreferencesKey("qada_ogle")
        val QADA_IKINDI = intPreferencesKey("qada_ikindi")
        val QADA_AKSAM = intPreferencesKey("qada_aksam")
        val QADA_YATSI = intPreferencesKey("qada_yatsi")
        val QADA_VITIR = intPreferencesKey("qada_vitir")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        mapPreferences(prefs)
    }

    suspend fun currentSettings(): UserSettings = settingsFlow.first()

    suspend fun setLocation(location: CityLocation) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOC_ID] = location.id
            prefs[Keys.LOC_NAME] = location.name
            prefs[Keys.LOC_COUNTRY] = location.country
            prefs[Keys.LOC_LAT] = location.latitude
            prefs[Keys.LOC_LON] = location.longitude
            prefs[Keys.LOC_TZ] = location.timeZoneId
            prefs[Keys.LOC_IS_GPS] = location.isGps
        }
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { it[Keys.CALC_METHOD] = method.name }
    }

    suspend fun setMadhab(madhab: AsrMadhab) {
        context.dataStore.edit { it[Keys.MADHAB] = madhab.name }
    }

    suspend fun setHighLatitudeRule(rule: HighLatitudeRule) {
        context.dataStore.edit { it[Keys.HIGH_LAT] = rule.name }
    }

    suspend fun setAdjustments(adjustments: PrayerAdjustments) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ADJ_IMSAK] = adjustments.imsak
            prefs[Keys.ADJ_GUNES] = adjustments.gunes
            prefs[Keys.ADJ_OGLE] = adjustments.ogle
            prefs[Keys.ADJ_IKINDI] = adjustments.ikindi
            prefs[Keys.ADJ_AKSAM] = adjustments.aksam
            prefs[Keys.ADJ_YATSI] = adjustments.yatsi
        }
    }

    suspend fun setHijriDayOffset(offset: Int) {
        context.dataStore.edit { it[Keys.HIJRI_OFFSET] = offset.coerceIn(-2, 2) }
    }

    suspend fun setShowKerahatBanner(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_KERAHAT] = show }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPrayerNotificationEnabled(type: PrayerType, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[notifKey(type)] = enabled
        }
    }

    suspend fun setNotificationLeadMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.LEAD_MINUTES] = minutes.coerceAtLeast(0) }
    }

    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
            if (!enabled) {
                prefs[Keys.SOUND_PRESET] = NotificationSoundPreset.SILENT.name
            } else {
                val cur = prefs[Keys.SOUND_PRESET]
                if (cur == null || cur == NotificationSoundPreset.SILENT.name) {
                    prefs[Keys.SOUND_PRESET] = NotificationSoundPreset.MELODI_1.name
                }
            }
        }
    }

    suspend fun setNotificationSoundPreset(preset: NotificationSoundPreset) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_PRESET] = preset.name
            prefs[Keys.SOUND_ENABLED] = preset != NotificationSoundPreset.SILENT
        }
    }

    suspend fun setNotificationSoundDurationSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_DURATION_SECONDS] = seconds.coerceIn(
                UserSettings.MIN_SOUND_DURATION_SECONDS,
                UserSettings.MAX_SOUND_DURATION_SECONDS
            )
        }
    }

    suspend fun setCustomNotificationSound(uri: String, displayName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_SOUND_URI] = uri
            prefs[Keys.CUSTOM_SOUND_NAME] = displayName
            prefs[Keys.SOUND_PRESET] = NotificationSoundPreset.CUSTOM_URI.name
            prefs[Keys.SOUND_ENABLED] = true
        }
    }

    suspend fun saveDhikrState(
        list: List<DhikrItem>,
        selectedId: String,
        totalDelta: Long = 0L
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DHIKR_JSON] = encodeDhikrs(list)
            prefs[Keys.SELECTED_DHIKR_ID] = selectedId
            if (totalDelta != 0L) {
                val current = prefs[Keys.TOTAL_DHIKR] ?: 0L
                prefs[Keys.TOTAL_DHIKR] = (current + totalDelta).coerceAtLeast(0L)
            }
        }
    }

    suspend fun setDhikrHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DHIKR_HAPTIC] = enabled }
    }

    suspend fun updateQadaCount(type: QadaPrayerType, newCount: Int) {
        context.dataStore.edit { prefs ->
            prefs[qadaKey(type)] = newCount.coerceAtLeast(0)
        }
    }

    suspend fun resetAllQada() {
        context.dataStore.edit { prefs ->
            QadaPrayerType.entries.forEach { prefs[qadaKey(it)] = 0 }
        }
    }

    private fun notifKey(type: PrayerType) = when (type) {
        PrayerType.IMSAK -> Keys.NOTIF_IMSAK
        PrayerType.GUNES -> Keys.NOTIF_GUNES
        PrayerType.OGLE -> Keys.NOTIF_OGLE
        PrayerType.IKINDI -> Keys.NOTIF_IKINDI
        PrayerType.AKSAM -> Keys.NOTIF_AKSAM
        PrayerType.YATSI -> Keys.NOTIF_YATSI
    }

    private fun qadaKey(type: QadaPrayerType) = when (type) {
        QadaPrayerType.SABAH -> Keys.QADA_SABAH
        QadaPrayerType.OGLE -> Keys.QADA_OGLE
        QadaPrayerType.IKINDI -> Keys.QADA_IKINDI
        QadaPrayerType.AKSAM -> Keys.QADA_AKSAM
        QadaPrayerType.YATSI -> Keys.QADA_YATSI
        QadaPrayerType.VITIR -> Keys.QADA_VITIR
    }

    private fun mapPreferences(prefs: Preferences): UserSettings {
        val defaultCity = CityCatalog.DEFAULT_CITY
        val location = CityLocation(
            id = prefs[Keys.LOC_ID] ?: defaultCity.id,
            name = prefs[Keys.LOC_NAME] ?: defaultCity.name,
            country = prefs[Keys.LOC_COUNTRY] ?: defaultCity.country,
            latitude = prefs[Keys.LOC_LAT] ?: defaultCity.latitude,
            longitude = prefs[Keys.LOC_LON] ?: defaultCity.longitude,
            timeZoneId = prefs[Keys.LOC_TZ] ?: defaultCity.timeZoneId,
            isGps = prefs[Keys.LOC_IS_GPS] ?: false
        )

        val method = prefs[Keys.CALC_METHOD]?.let { name ->
            CalculationMethod.entries.firstOrNull { it.name == name }
        } ?: CalculationMethod.DIYANET

        val madhab = prefs[Keys.MADHAB]?.let { name ->
            AsrMadhab.entries.firstOrNull { it.name == name }
        } ?: AsrMadhab.STANDARD

        val highLat = prefs[Keys.HIGH_LAT]?.let { name ->
            HighLatitudeRule.entries.firstOrNull { it.name == name }
        } ?: HighLatitudeRule.MIDDLE_OF_NIGHT

        val themeMode = prefs[Keys.THEME_MODE]?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }
        } ?: ThemeMode.SYSTEM

        val adjustments = PrayerAdjustments(
            imsak = prefs[Keys.ADJ_IMSAK] ?: 0,
            gunes = prefs[Keys.ADJ_GUNES] ?: 0,
            ogle = prefs[Keys.ADJ_OGLE] ?: 0,
            ikindi = prefs[Keys.ADJ_IKINDI] ?: 0,
            aksam = prefs[Keys.ADJ_AKSAM] ?: 0,
            yatsi = prefs[Keys.ADJ_YATSI] ?: 0
        )

        val defaults = UserSettings.defaultNotificationMap()
        val notifs = PrayerType.entries.associateWith { type ->
            prefs[notifKey(type)] ?: (defaults[type] ?: true)
        }

        val dhikrList = prefs[Keys.DHIKR_JSON]?.let(::decodeDhikrs)
            ?.takeIf { it.isNotEmpty() }
            ?: UserSettings.defaultDhikrPresets()

        val selectedDhikr = prefs[Keys.SELECTED_DHIKR_ID]
            ?.takeIf { id -> dhikrList.any { it.id == id } }
            ?: dhikrList.first().id

        val qadaCounts = QadaPrayerType.entries.associateWith { type ->
            (prefs[qadaKey(type)] ?: 0).coerceAtLeast(0)
        }

        val soundEnabledLegacy = prefs[Keys.SOUND_ENABLED] ?: true
        val soundPreset = prefs[Keys.SOUND_PRESET]?.let { rawName ->
            val mappedName = when (rawName) {
                "GENTLE_MELODY" -> NotificationSoundPreset.MELODI_1.name
                "NEY_HICAZ" -> NotificationSoundPreset.NEY_1.name
                "KANUN_ACOUSTIC" -> NotificationSoundPreset.KANUN_1.name
                "TASAVVUF_AMBIENT" -> NotificationSoundPreset.TASAVVUF_1.name
                "SABAH_MAKAM" -> NotificationSoundPreset.SABAH.name
                "CLASSIC_CHIME" -> NotificationSoundPreset.ALARM.name
                else -> rawName
            }
            NotificationSoundPreset.entries.firstOrNull { it.name == mappedName }
        } ?: if (soundEnabledLegacy) {
            NotificationSoundPreset.MELODI_1
        } else {
            NotificationSoundPreset.SILENT
        }

        val soundDurationSeconds = (
            prefs[Keys.SOUND_DURATION_SECONDS] ?: UserSettings.DEFAULT_SOUND_DURATION_SECONDS
        ).coerceIn(
            UserSettings.MIN_SOUND_DURATION_SECONDS,
            UserSettings.MAX_SOUND_DURATION_SECONDS
        )

        return UserSettings(
            location = location,
            calculationMethod = method,
            madhab = madhab,
            highLatitudeRule = highLat,
            adjustments = adjustments,
            hijriDayOffset = (prefs[Keys.HIJRI_OFFSET] ?: 0).coerceIn(-2, 2),
            showKerahatBanner = prefs[Keys.SHOW_KERAHAT] ?: true,
            themeMode = themeMode,
            enabledNotifications = notifs,
            notificationLeadMinutes = prefs[Keys.LEAD_MINUTES] ?: 0,
            notificationSoundEnabled = soundPreset != NotificationSoundPreset.SILENT,
            notificationSoundPreset = soundPreset,
            notificationSoundDurationSeconds = soundDurationSeconds,
            customNotificationSoundUri = prefs[Keys.CUSTOM_SOUND_URI] ?: "",
            customNotificationSoundName = prefs[Keys.CUSTOM_SOUND_NAME] ?: "",
            dhikrList = dhikrList,
            selectedDhikrId = selectedDhikr,
            dhikrHapticEnabled = prefs[Keys.DHIKR_HAPTIC] ?: true,
            totalDhikrCount = prefs[Keys.TOTAL_DHIKR] ?: 0L,
            qadaCounts = qadaCounts
        )
    }

    private fun encodeDhikrs(list: List<DhikrItem>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("subtitle", item.subtitle)
            obj.put("count", item.count)
            obj.put("target", item.target)
            obj.put("isPreset", item.isPreset)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeDhikrs(raw: String): List<DhikrItem> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        DhikrItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            subtitle = obj.optString("subtitle", ""),
                            count = obj.optInt("count", 0).coerceAtLeast(0),
                            target = obj.optInt("target", 33).coerceAtLeast(1),
                            isPreset = obj.optBoolean("isPreset", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
