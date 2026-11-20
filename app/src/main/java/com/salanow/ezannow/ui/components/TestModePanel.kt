package com.salanow.ezannow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salanow.ezannow.domain.testmode.SimulationSpeed
import com.salanow.ezannow.domain.testmode.TestModeState
import com.salanow.ezannow.notification.PrayerNotificationHelper
import com.salanow.ezannow.ui.theme.TabularTimeStyle

@Composable
fun TestModePanel(
    testMode: TestModeState,
    onTogglePlayPause: () -> Unit,
    onMinutesChange: (Float) -> Unit,
    onSelectSpeed: (SimulationSpeed) -> Unit,
    onResetClock: () -> Unit,
    onJumpBeforeNextPrayer: () -> Unit,
    onCloseTestMode: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val hasNotifPerm = PrayerNotificationHelper.hasNotificationPermission(context)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = "TEST MODU (0-24s)",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = testMode.formattedClock(),
                        style = TabularTimeStyle,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                TextButton(
                    onClick = onCloseTestMode,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Kapat", fontWeight = FontWeight.SemiBold)
                }
            }

            Column {
                Slider(
                    value = testMode.minutesOfDayFloat(),
                    onValueChange = onMinutesChange,
                    valueRange = 0f..1440f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "00:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Hız: ${testMode.speed.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "24:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (testMode.isPlaying) "Duraklat" else "Başlat",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onResetClock,
                    modifier = Modifier.weight(0.85f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("00:00", maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onJumpBeforeNextPrayer,
                    modifier = Modifier.weight(1.15f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Vakte -1 dk", maxLines = 1, fontSize = 12.sp)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(SimulationSpeed.entries, key = { it.name }) { spd ->
                    FilterChip(
                        selected = testMode.speed == spd,
                        onClick = { onSelectSpeed(spd) },
                        label = { Text(spd.label, fontSize = 12.sp) }
                    )
                }
            }

            if (!hasNotifPerm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bildirim görmek için izin verin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = onRequestNotificationPermission,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Bildirim İzni Ver", fontSize = 12.sp)
                    }
                }
            }

            if (testMode.triggeredEvents.isNotEmpty()) {
                val summary = testMode.triggeredEvents
                    .take(5)
                    .joinToString(" • ") { "${it.prayer.turkishName} ${it.prayerTimeFormatted}" }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bildirimler (${testMode.triggeredEvents.size}): $summary",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
