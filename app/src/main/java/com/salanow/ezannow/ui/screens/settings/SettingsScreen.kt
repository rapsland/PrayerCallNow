package com.salanow.ezannow.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salanow.ezannow.BuildConfig
import com.salanow.ezannow.data.preferences.UserSettings
import com.salanow.ezannow.domain.model.AsrMadhab
import com.salanow.ezannow.domain.model.CalculationMethod
import com.salanow.ezannow.domain.model.HighLatitudeRule
import com.salanow.ezannow.domain.model.NotificationSoundPreset
import com.salanow.ezannow.domain.model.PrayerAdjustments
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.ThemeMode
import com.salanow.ezannow.notification.PrayerAlarmScheduler
import com.salanow.ezannow.notification.PrayerNotificationHelper
import com.salanow.ezannow.ui.MainUiState
import com.salanow.ezannow.ui.components.EzanIcons

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onOpenCityPicker: () -> Unit,
    onDetectGps: () -> Unit,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectMadhab: (AsrMadhab) -> Unit,
    onSelectHighLat: (HighLatitudeRule) -> Unit,
    onSaveAdjustments: (PrayerAdjustments) -> Unit,
    onSetHijriOffset: (Int) -> Unit,
    onToggleKerahatBanner: (Boolean) -> Unit,
    onTogglePrayerNotif: (PrayerType) -> Unit,
    onSetLeadMinutes: (Int) -> Unit,
    onToggleNotificationSound: (Boolean) -> Unit,
    onTogglePersistentStatusNotif: (Boolean) -> Unit,
    onSelectSoundPreset: (NotificationSoundPreset) -> Unit,
    onSetSoundDurationSeconds: (Int) -> Unit,
    onPickCustomSound: () -> Unit,
    onPreviewSound: () -> Unit,
    onSendTestNotification: () -> Unit,
    onScheduleBackgroundSwipeTest: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleTestMode: (Boolean) -> Unit,
    onLaunchTestModeOnHome: () -> Unit,
    onReplaySplash: () -> Unit
) {
    val context = LocalContext.current
    val settings = uiState.settings

    var showMethodDialog by remember { mutableStateOf(false) }
    var showMadhabDialog by remember { mutableStateOf(false) }
    var showHighLatDialog by remember { mutableStateOf(false) }
    var showAdjustmentsDialog by remember { mutableStateOf(false) }
    var showLeadMinutesDialog by remember { mutableStateOf(false) }
    var showSoundPickerDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val hasNotifPermission = PrayerNotificationHelper.hasNotificationPermission(context)
    val canExactAlarm = PrayerAlarmScheduler.canScheduleExact(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Konum Section
        item {
            SettingsSectionCard(title = "Konum") {
                SettingsActionRow(
                    label = "Seçili Şehir",
                    value = "${settings.location.name} (${settings.location.country})",
                    onClick = onOpenCityPicker
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsActionRow(
                    label = "GPS ile Otomatik Bul",
                    value = if (uiState.isDetectingLocation) "Konum alınıyor..." else "Cihaz konumunu kullan",
                    onClick = onDetectGps
                )
            }
        }

        // Hesaplama Section
        item {
            SettingsSectionCard(title = "Vakit Hesaplama") {
                SettingsActionRow(
                    label = "Hesaplama Yöntemi",
                    value = settings.calculationMethod.turkishName,
                    onClick = { showMethodDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsActionRow(
                    label = "İkindi Mezhebi",
                    value = settings.madhab.turkishName,
                    onClick = { showMadhabDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsActionRow(
                    label = "Yüksek Enlem Kuralı",
                    value = settings.highLatitudeRule.turkishName,
                    onClick = { showHighLatDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsActionRow(
                    label = "Dakika Düzeltmeleri",
                    value = formatAdjustmentsSummary(settings.adjustments),
                    onClick = { showAdjustmentsDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hicri Tarih Düzeltmesi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${if (settings.hijriDayOffset > 0) "+" else ""}${settings.hijriDayOffset} gün",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onSetHijriOffset(settings.hijriDayOffset - 1) },
                            enabled = settings.hijriDayOffset > -2
                        ) { Text("-", style = MaterialTheme.typography.titleLarge) }
                        Text(
                            text = settings.hijriDayOffset.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = { onSetHijriOffset(settings.hijriDayOffset + 1) },
                            enabled = settings.hijriDayOffset < 2
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        }

        // Bildirimler Section
        item {
            SettingsSectionCard(title = "Bildirimler ve Uyarılar") {
                if (!hasNotifPermission) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vakit uyarıları için bildirim izni gerekli.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(onClick = onRequestNotificationPermission) {
                            Text("İzin Ver")
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }

                if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tam zamanında alarm için izin önerilir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        }) { Text("Ayarla") }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }

                SettingsActionRow(
                    label = "Önceden Hatırlat",
                    value = if (settings.notificationLeadMinutes == 0) {
                        "Tam vaktinde"
                    } else {
                        "${settings.notificationLeadMinutes} dakika önce"
                    },
                    onClick = { showLeadMinutesDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                PrayerType.entries.forEach { prayer ->
                    val enabled = settings.enabledNotifications[prayer] ?: true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePrayerNotif(prayer) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${prayer.turkishName} Bildirimi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { onTogglePrayerNotif(prayer) }
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleKerahatBanner(!settings.showKerahatBanner) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kerahat Vakti Uyarısı",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = settings.showKerahatBanner,
                        onCheckedChange = onToggleKerahatBanner
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onTogglePersistentStatusNotif(!settings.persistentStatusNotificationEnabled)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Durum Çubuğunda Vakit Göster",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Üst çubukta simge ve sıradaki vakte geri sayım (pil dostu)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.persistentStatusNotificationEnabled,
                        onCheckedChange = onTogglePersistentStatusNotif
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                val currentSoundLabel = if (
                    settings.notificationSoundPreset == NotificationSoundPreset.CUSTOM_URI &&
                    settings.customNotificationSoundName.isNotBlank()
                ) {
                    "Özel: ${settings.customNotificationSoundName}"
                } else {
                    settings.notificationSoundPreset.turkishName
                }
                SettingsActionRow(
                    label = "Bildirim Sesi (${settings.notificationSoundDurationSeconds} sn)",
                    value = currentSoundLabel,
                    onClick = { showSoundPickerDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bildirim Sesi Süresi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${settings.notificationSoundDurationSeconds} saniye (Varsayılan: 4 sn, Maks: 10 sn)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                onSetSoundDurationSeconds(
                                    settings.notificationSoundDurationSeconds - 1
                                )
                            },
                            enabled = settings.notificationSoundDurationSeconds > UserSettings.MIN_SOUND_DURATION_SECONDS
                        ) { Text("-", style = MaterialTheme.typography.titleLarge) }
                        Text(
                            text = "${settings.notificationSoundDurationSeconds} sn",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = {
                                onSetSoundDurationSeconds(
                                    settings.notificationSoundDurationSeconds + 1
                                )
                            },
                            enabled = settings.notificationSoundDurationSeconds < UserSettings.MAX_SOUND_DURATION_SECONDS
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleNotificationSound(!settings.notificationSoundEnabled) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bildirim Sesi Açık",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kapalıyken sadece sessiz titreşimle bildirir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.notificationSoundEnabled,
                        onCheckedChange = onToggleNotificationSound
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onPreviewSound) {
                            Text("Sesi Dinle")
                        }
                        FilledTonalButton(onClick = onSendTestNotification) {
                            Icon(
                                imageVector = EzanIcons.BellOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Bildirimi Gönder")
                        }
                    }
                    OutlinedButton(
                        onClick = onScheduleBackgroundSwipeTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("10 sn Arka Plan Testi (Uygulamayı Kapat)")
                    }
                    Text(
                        text = "Alarmı 10 sn sonraya kurar; uygulamayı kaydırarak kapatsanız da bildirim ve ses çalar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Test Modu Section
        item {
            SettingsSectionCard(title = "Test Modu (0-24 Saat)") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTestMode(!uiState.testMode.enabled) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zaman Simülasyonu (0-24s)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vakit geçişlerini ve bildirimleri hızlandırılmış test et",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.testMode.enabled,
                        onCheckedChange = onToggleTestMode
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsActionRow(
                    label = "Vakitler Ekranında Testi Başlat",
                    value = "Zaman çubuğu ile 00:00 - 24:00 arası simülasyon",
                    onClick = onLaunchTestModeOnHome
                )
            }
        }

        // Görünüm & Hakkında Section
        item {
            SettingsSectionCard(title = "Görünüm ve Hakkında") {
                SettingsActionRow(
                    label = "Tema",
                    value = settings.themeMode.turkishName,
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsActionRow(
                    label = "Açılış Animasyonunu İzle",
                    value = "Galaksi, Nebula ve Zeus animasyonunu tekrar oynat",
                    onClick = onReplaySplash
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "EzanNow! v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Reklamsız, çevrimdışı ve hafif namaz vakitleri uygulaması.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showMethodDialog) {
        SingleSelectDialog(
            title = "Hesaplama Yöntemi",
            options = CalculationMethod.entries,
            selected = settings.calculationMethod,
            labelOf = { it.turkishName },
            onSelect = {
                onSelectMethod(it)
                showMethodDialog = false
            },
            onDismiss = { showMethodDialog = false }
        )
    }

    if (showMadhabDialog) {
        SingleSelectDialog(
            title = "İkindi Mezhebi",
            options = AsrMadhab.entries,
            selected = settings.madhab,
            labelOf = { it.turkishName },
            onSelect = {
                onSelectMadhab(it)
                showMadhabDialog = false
            },
            onDismiss = { showMadhabDialog = false }
        )
    }

    if (showHighLatDialog) {
        SingleSelectDialog(
            title = "Yüksek Enlem Kuralı",
            options = HighLatitudeRule.entries,
            selected = settings.highLatitudeRule,
            labelOf = { it.turkishName },
            onSelect = {
                onSelectHighLat(it)
                showHighLatDialog = false
            },
            onDismiss = { showHighLatDialog = false }
        )
    }

    if (showLeadMinutesDialog) {
        val leadOptions = listOf(0, 5, 10, 15, 30)
        SingleSelectDialog(
            title = "Önceden Hatırlat",
            options = leadOptions,
            selected = settings.notificationLeadMinutes,
            labelOf = { if (it == 0) "Tam vaktinde" else "$it dakika önce" },
            onSelect = {
                onSetLeadMinutes(it)
                showLeadMinutesDialog = false
            },
            onDismiss = { showLeadMinutesDialog = false }
        )
    }

    if (showSoundPickerDialog) {
        SoundPickerDialog(
            selected = settings.notificationSoundPreset,
            soundDurationSeconds = settings.notificationSoundDurationSeconds,
            customSoundUri = settings.customNotificationSoundUri,
            customSoundName = settings.customNotificationSoundName,
            onSelectPreset = onSelectSoundPreset,
            onSetDurationSeconds = onSetSoundDurationSeconds,
            onPickCustomSound = onPickCustomSound,
            onDismiss = { showSoundPickerDialog = false }
        )
    }

    if (showThemeDialog) {
        SingleSelectDialog(
            title = "Tema Seçimi",
            options = ThemeMode.entries,
            selected = settings.themeMode,
            labelOf = { it.turkishName },
            onSelect = {
                onSelectTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAdjustmentsDialog) {
        AdjustmentsDialog(
            initial = settings.adjustments,
            onSave = {
                onSaveAdjustments(it)
                showAdjustmentsDialog = false
            },
            onDismiss = { showAdjustmentsDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = EzanIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SoundPickerDialog(
    selected: NotificationSoundPreset,
    soundDurationSeconds: Int,
    customSoundUri: String,
    customSoundName: String,
    onSelectPreset: (NotificationSoundPreset) -> Unit,
    onSetDurationSeconds: (Int) -> Unit,
    onPickCustomSound: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = NotificationSoundPreset.entries.filter { preset ->
        preset != NotificationSoundPreset.CUSTOM_URI || customSoundUri.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bildirim Sesi Seçin") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Çalma Süresi",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$soundDurationSeconds sn",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = soundDurationSeconds.toFloat(),
                            onValueChange = { onSetDurationSeconds(kotlin.math.round(it).toInt()) },
                            valueRange = UserSettings.MIN_SOUND_DURATION_SECONDS.toFloat()..
                                UserSettings.MAX_SOUND_DURATION_SECONDS.toFloat(),
                            steps = UserSettings.MAX_SOUND_DURATION_SECONDS -
                                UserSettings.MIN_SOUND_DURATION_SECONDS - 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                options.forEach { preset ->
                    val label = if (preset == NotificationSoundPreset.CUSTOM_URI && customSoundName.isNotBlank()) {
                        "Özel Ses: $customSoundName"
                    } else {
                        preset.turkishName
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPreset(preset) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preset == selected,
                            onClick = { onSelectPreset(preset) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onPickCustomSound,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Telefondan Ses Seç (MP3/WAV)...")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tamam") }
        }
    )
}

@Composable
private fun <T> SingleSelectDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(labelOf(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@Composable
private fun AdjustmentsDialog(
    initial: PrayerAdjustments,
    onSave: (PrayerAdjustments) -> Unit,
    onDismiss: () -> Unit
) {
    var imsak by remember { mutableIntStateOf(initial.imsak) }
    var gunes by remember { mutableIntStateOf(initial.gunes) }
    var ogle by remember { mutableIntStateOf(initial.ogle) }
    var ikindi by remember { mutableIntStateOf(initial.ikindi) }
    var aksam by remember { mutableIntStateOf(initial.aksam) }
    var yatsi by remember { mutableIntStateOf(initial.yatsi) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dakika Düzeltmeleri") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdjustmentStepper("İmsak", imsak) { imsak = it }
                AdjustmentStepper("Güneş", gunes) { gunes = it }
                AdjustmentStepper("Öğle", ogle) { ogle = it }
                AdjustmentStepper("İkindi", ikindi) { ikindi = it }
                AdjustmentStepper("Akşam", aksam) { aksam = it }
                AdjustmentStepper("Yatsı", yatsi) { yatsi = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(PrayerAdjustments(imsak, gunes, ogle, ikindi, aksam, yatsi))
            }) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = {
                imsak = 0; gunes = 0; ogle = 0; ikindi = 0; aksam = 0; yatsi = 0
            }) { Text("Sıfırla") }
        }
    )
}

@Composable
private fun AdjustmentStepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange((value - 1).coerceIn(-30, 30)) },
                modifier = Modifier.size(34.dp)
            ) { Text("-", fontWeight = FontWeight.Bold) }
            Text(
                text = "${if (value > 0) "+" else ""}$value dk",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(52.dp)
            )
            IconButton(
                onClick = { onChange((value + 1).coerceIn(-30, 30)) },
                modifier = Modifier.size(34.dp)
            ) { Text("+", fontWeight = FontWeight.Bold) }
        }
    }
}

private fun formatAdjustmentsSummary(adj: PrayerAdjustments): String {
    val nonZero = PrayerType.entries
        .mapNotNull { type ->
            val v = adj.forPrayer(type)
            if (v != 0) "${type.shortName} ${if (v > 0) "+" else ""}$v" else null
        }
    return if (nonZero.isEmpty()) "Yok (0 dk)" else nonZero.joinToString(", ")
}
