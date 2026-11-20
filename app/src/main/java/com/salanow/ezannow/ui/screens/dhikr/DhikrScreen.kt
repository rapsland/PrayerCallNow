package com.salanow.ezannow.ui.screens.dhikr

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salanow.ezannow.domain.model.QadaPrayerType
import com.salanow.ezannow.ui.MainUiState
import com.salanow.ezannow.ui.components.EzanIcons
import com.salanow.ezannow.ui.theme.CountdownTextStyle
import com.salanow.ezannow.ui.theme.EzanGold

@Composable
fun DhikrScreen(
    uiState: MainUiState,
    onSelectDhikr: (String) -> Unit,
    onIncrementDhikr: () -> Unit,
    onDecrementDhikr: () -> Unit,
    onResetCurrentDhikr: () -> Unit,
    onAddCustomDhikr: (String, String, Int) -> Unit,
    onDeleteCustomDhikr: (String) -> Unit,
    onToggleHaptic: () -> Unit,
    onAdjustQada: (QadaPrayerType, Int) -> Unit,
    onResetAllQada: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Zikirmatik, 1 = Kaza Takibi

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (selectedTab == 0) "Zikirmatik" else "Kaza Takibi",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Pill Switcher
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Zikirmatik", "Kaza Takibi").forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = idx },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            ZikirmatikContent(
                uiState = uiState,
                onSelectDhikr = onSelectDhikr,
                onIncrementDhikr = onIncrementDhikr,
                onDecrementDhikr = onDecrementDhikr,
                onResetCurrentDhikr = onResetCurrentDhikr,
                onAddCustomDhikr = onAddCustomDhikr,
                onDeleteCustomDhikr = onDeleteCustomDhikr,
                onToggleHaptic = onToggleHaptic
            )
        } else {
            KazaTrackerContent(
                qadaCounts = uiState.settings.qadaCounts,
                onAdjustQada = onAdjustQada,
                onResetAllQada = onResetAllQada
            )
        }
    }
}

@Composable
private fun ZikirmatikContent(
    uiState: MainUiState,
    onSelectDhikr: (String) -> Unit,
    onIncrementDhikr: () -> Unit,
    onDecrementDhikr: () -> Unit,
    onResetCurrentDhikr: () -> Unit,
    onAddCustomDhikr: (String, String, Int) -> Unit,
    onDeleteCustomDhikr: (String) -> Unit,
    onToggleHaptic: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val settings = uiState.settings
    val activeDhikr = settings.dhikrList.firstOrNull { it.id == settings.selectedDhikrId }
        ?: settings.dhikrList.first()

    var showAddDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val cycleCount = activeDhikr.count % activeDhikr.target
    val targetFraction = if (activeDhikr.count > 0 && cycleCount == 0) {
        1f
    } else {
        (cycleCount.toFloat() / activeDhikr.target.toFloat()).coerceIn(0f, 1f)
    }
    val animatedFraction by animateFloatAsState(targetValue = targetFraction, label = "dhikrArc")
    val roundsCompleted = activeDhikr.count / activeDhikr.target

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal Dhikr Selector + Add button
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(settings.dhikrList, key = { it.id }) { item ->
                val selected = item.id == activeDhikr.id
                FilterChip(
                    selected = selected,
                    onClick = { onSelectDhikr(item.id) },
                    label = { Text(item.title) },
                    trailingIcon = if (!item.isPreset) {
                        {
                            IconButton(
                                onClick = { onDeleteCustomDhikr(item.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = EzanIcons.Close,
                                    contentDescription = "Sil",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    } else null
                )
            }

            item {
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = EzanIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekle")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Dhikr Card Info
        Text(
            text = activeDhikr.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (activeDhikr.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activeDhikr.subtitle,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Giant Tap Circle
        val primaryColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val ringBg = MaterialTheme.colorScheme.surface

        Box(
            modifier = Modifier
                .size(236.dp)
                .clip(CircleShape)
                .clickable {
                    if (settings.dhikrHapticEnabled) {
                        val isTargetHit = (activeDhikr.count + 1) % activeDhikr.target == 0
                        haptic.performHapticFeedback(
                            if (isTargetHit) HapticFeedbackType.LongPress
                            else HapticFeedbackType.TextHandleMove
                        )
                    }
                    onIncrementDhikr()
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                drawCircle(color = ringBg, radius = size.minDimension / 2f - stroke)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = if (targetFraction >= 1f) EzanGold else primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeDhikr.count.toString(),
                    style = CountdownTextStyle.copy(fontSize = 46.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hedef: ${activeDhikr.target}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (roundsCompleted > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$roundsCompleted Tur",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDecrementDhikr,
                enabled = activeDhikr.count > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("-1 Geri Al")
            }

            OutlinedButton(
                onClick = { showResetConfirm = true },
                enabled = activeDhikr.count > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = EzanIcons.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sıfırla")
            }

            FilledTonalButton(
                onClick = onToggleHaptic,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (settings.dhikrHapticEnabled) "Titreşim: Açık" else "Titreşim: Kapalı",
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toplam Çekilen Zikir",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = settings.totalDhikrCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Sayacı Sıfırla") },
            text = { Text("\"${activeDhikr.title}\" sayacı sıfırlansın mı?") },
            confirmButton = {
                TextButton(onClick = {
                    onResetCurrentDhikr()
                    showResetConfirm = false
                }) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("İptal") }
            }
        )
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("33") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Zikir Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Zikir Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Arapça / Açıklama (İsteğe bağlı)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { txt -> targetText = txt.filter { it.isDigit() } },
                        label = { Text("Hedef Sayı") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetText.toIntOrNull()?.coerceIn(1, 10_000) ?: 33
                        onAddCustomDhikr(title, subtitle, target)
                        showAddDialog = false
                    },
                    enabled = title.trim().isNotEmpty()
                ) { Text("Ekle") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun KazaTrackerContent(
    qadaCounts: Map<QadaPrayerType, Int>,
    onAdjustQada: (QadaPrayerType, Int) -> Unit,
    onResetAllQada: () -> Unit
) {
    val totalRemaining = qadaCounts.values.sum()
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Toplam Kalan Kaza",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$totalRemaining vakit",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (totalRemaining > 0) {
                        TextButton(onClick = { showResetConfirm = true }) {
                            Text("Tümünü Sıfırla")
                        }
                    }
                }
            }
        }

        items(QadaPrayerType.entries, key = { it.name }) { type ->
            val count = qadaCounts[type] ?: 0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.turkishName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kalan: $count",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustQada(type, -1) },
                            enabled = count > 0,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) { Text("-1", fontWeight = FontWeight.Bold) }

                        OutlinedButton(
                            onClick = { onAdjustQada(type, 1) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) { Text("+1", fontWeight = FontWeight.Bold) }

                        FilledTonalButton(
                            onClick = { onAdjustQada(type, 30) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) { Text("+30", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Kaza Sayacını Sıfırla") },
            text = { Text("Tüm kaza namazı sayıları sıfırlansın mı?") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAllQada()
                    showResetConfirm = false
                }) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("İptal") }
            }
        )
    }
}
