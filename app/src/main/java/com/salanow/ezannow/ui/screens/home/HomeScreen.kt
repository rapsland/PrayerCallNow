package com.salanow.ezannow.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salanow.ezannow.domain.model.KerahatStatus
import com.salanow.ezannow.domain.model.PrayerTimesDay
import com.salanow.ezannow.domain.model.PrayerType
import com.salanow.ezannow.domain.model.SimpleDate
import com.salanow.ezannow.domain.testmode.SimulationSpeed
import com.salanow.ezannow.ui.MainUiState
import com.salanow.ezannow.ui.components.EzanIcons
import com.salanow.ezannow.ui.components.TestModePanel
import com.salanow.ezannow.ui.theme.CountdownTextStyle
import com.salanow.ezannow.ui.theme.LocalEzanColors
import com.salanow.ezannow.ui.theme.TabularTimeStyle
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onOpenCityPicker: () -> Unit,
    onDetectGps: () -> Unit,
    onSelectDate: (SimpleDate) -> Unit,
    onJumpToToday: () -> Unit,
    onTogglePrayerNotification: (PrayerType) -> Unit,
    onToggleCustomTestTime: (Boolean) -> Unit,
    onToggleTestPlayPause: () -> Unit,
    onTestMinutesChange: (Float) -> Unit,
    onTestSpeedSelect: (SimulationSpeed) -> Unit,
    onTestResetClock: () -> Unit,
    onTestJumpBeforeNextPrayer: () -> Unit,
    onTestClose: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val extColors = LocalEzanColors.current
    val isViewingToday = uiState.selectedDate == uiState.todayDate

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Location Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenCityPicker,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = EzanIcons.LocationPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.settings.location.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = EzanIcons.ChevronDown,
                            contentDescription = "Şehir Değiştir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.settings.calculationMethod.turkishName.let {
                            if (it.startsWith("Diyanet")) "Diyanet" else it.take(14)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = onDetectGps,
                            enabled = !uiState.isDetectingLocation,
                            modifier = Modifier.size(38.dp)
                        ) {
                            if (uiState.isDetectingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = EzanIcons.MyLocationGps,
                                    contentDescription = "GPS ile Bul",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Test Mode Simulation Controls (0-24h slow-motion)
        item {
            AnimatedVisibility(visible = uiState.testMode.enabled) {
                TestModePanel(
                    testMode = uiState.testMode,
                    onTogglePlayPause = onToggleTestPlayPause,
                    onMinutesChange = onTestMinutesChange,
                    onSelectSpeed = onTestSpeedSelect,
                    onResetClock = onTestResetClock,
                    onJumpBeforeNextPrayer = onTestJumpBeforeNextPrayer,
                    onCloseTestMode = onTestClose,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
            }
        }

        // Hero Countdown Card
        item {
            val nextInfo = uiState.nextPrayerInfo
            val nextTimeStr = PrayerTimesDay.formatMillis(
                nextInfo.nextPrayerTimeMillis,
                uiState.settings.location.timeZoneId
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(extColors.heroGradientStart, extColors.heroGradientEnd)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${nextInfo.nextPrayer.turkishName} vaktine kalan",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${nextInfo.nextPrayer.turkishName} $nextTimeStr",
                                color = extColors.goldAccent,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatCountdown(nextInfo.remainingMillis),
                        style = CountdownTextStyle,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { nextInfo.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = extColors.goldAccent,
                        trackColor = Color.White.copy(alpha = 0.22f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.selectedDate.formatFullTr(),
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = uiState.hijriDate.formatTr(),
                            color = extColors.goldAccent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Kerahat Banner
        item {
            AnimatedVisibility(visible = uiState.kerahatStatus !is KerahatStatus.None) {
                val status = uiState.kerahatStatus
                val bannerText = when (status) {
                    is KerahatStatus.Active ->
                        "${status.label} • ${status.remainingMinutes} dk kaldı"
                    is KerahatStatus.Upcoming ->
                        "${status.label} ${status.minutesUntilStart} dk içinde başlayacak"
                    KerahatStatus.None -> ""
                }
                if (bannerText.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = extColors.kerahatContainer,
                        border = BorderStroke(1.dp, extColors.kerahatBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = EzanIcons.KerahatWarning,
                                contentDescription = null,
                                tint = extColors.kerahatContent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = bannerText,
                                color = extColors.kerahatContent,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 7-Day Date Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isViewingToday) "Bugünün Vakitleri" else "${uiState.selectedDate.formatShortTr()} Vakitleri",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (!isViewingToday) {
                        TextButton(onClick = onJumpToToday) {
                            Text("Bugüne Dön", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.sevenDayStrip, key = { "${it.year}-${it.month}-${it.day}" }) { date ->
                        val isSelected = date == uiState.selectedDate
                        val isToday = date == uiState.todayDate
                        Surface(
                            modifier = Modifier
                                .width(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectDate(date) },
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isToday) "Bugün" else date.dayOfWeekShortTr(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = date.day.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6 Prayer Cards
        items(PrayerType.entries, key = { it.name }) { prayer ->
            val isActive = isViewingToday && uiState.nextPrayerInfo.currentPrayer == prayer
            val isNext = isViewingToday && uiState.nextPrayerInfo.nextPrayer == prayer
            val alarmEnabled = uiState.settings.enabledNotifications[prayer] ?: true
            val formattedTime = uiState.selectedDayPrayerTimes.formattedTime(prayer)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isNext -> extColors.activeCardContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (isNext) 1.5.dp else 1.dp,
                    color = if (isNext) extColors.activeCardBorder else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = EzanIcons.forPrayer(prayer),
                            contentDescription = null,
                            tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = prayer.turkishName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isNext || isActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isNext) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "Sıradaki",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "İçinde",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = formattedTime,
                        style = TabularTimeStyle,
                        color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { onTogglePrayerNotification(prayer) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (alarmEnabled) EzanIcons.BellOn else EzanIcons.BellOff,
                            contentDescription = if (alarmEnabled) "Bildirimi Kapat" else "Bildirimi Aç",
                            tint = if (alarmEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Optional Developer Custom Test Time Row
        if (uiState.settings.customTestTimeEnabled) {
            item(key = "custom_test_prayer_row") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = EzanIcons.BellOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Özel Test Vakti",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                ) {
                                    Text(
                                        text = "Geliştirici",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Zaman simülasyonu olmadan gerçek alarm testi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = uiState.settings.customTestTimeFormatted(),
                            style = TabularTimeStyle,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onToggleCustomTestTime(false) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = EzanIcons.BellOn,
                                contentDescription = "Özel Test Vaktini Kapat",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCountdown(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.forLanguageTag("tr-TR"), "%02d:%02d:%02d", hours, minutes, seconds)
}
