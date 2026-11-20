package com.salanow.ezannow.ui.screens.qibla

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salanow.ezannow.domain.calculator.QiblaCalculator
import com.salanow.ezannow.ui.MainUiState
import com.salanow.ezannow.ui.components.EzanIcons
import com.salanow.ezannow.ui.theme.EzanEmerald
import com.salanow.ezannow.ui.theme.EzanGold
import com.salanow.ezannow.ui.theme.LocalEzanColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    uiState: MainUiState,
    onOpenCityPicker: () -> Unit
) {
    val extColors = LocalEzanColors.current
    val haptic = LocalHapticFeedback.current
    val loc = uiState.settings.location
    val bearing = uiState.qiblaBearing.toFloat()
    val compass by rememberCompassReading(loc.latitude, loc.longitude)

    val delta = if (compass.hasSensor) {
        QiblaCalculator.shortestDeltaDegrees(compass.trueHeadingDegrees, bearing)
    } else {
        0f
    }
    val aligned = compass.hasSensor && abs(delta) <= QiblaCalculator.ALIGN_THRESHOLD_DEGREES

    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(aligned) {
        if (aligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = aligned
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Kıble Pusulası",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = loc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = onOpenCityPicker,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = EzanIcons.LocationPin,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Değiştir", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Alignment Status Pill
        val statusText = when {
            !compass.hasSensor -> "Cihazda pusula sensörü bulunamadı — sabit kıble açısı"
            aligned -> "Kıbleye Hizalandı ✓"
            delta > 0f -> "Sağa dönün (${delta.roundToInt()}°)"
            else -> "Sola dönün (${abs(delta).roundToInt()}°)"
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = when {
                !compass.hasSensor -> MaterialTheme.colorScheme.surfaceVariant
                aligned -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            Text(
                text = statusText,
                color = if (aligned) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Compass Dial Canvas
        Box(
            modifier = Modifier.size(290.dp),
            contentAlignment = Alignment.Center
        ) {
            val dialRotation = if (compass.hasSensor) -compass.trueHeadingDegrees else 0f
            val dialSurface = MaterialTheme.colorScheme.surface

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer glow ring when aligned
                drawCircle(
                    color = if (aligned) EzanEmerald else mutedColor.copy(alpha = 0.25f),
                    radius = radius + 6.dp.toPx(),
                    center = center,
                    style = Stroke(width = if (aligned) 5.dp.toPx() else 2.dp.toPx())
                )

                // Inner dial plate
                drawCircle(
                    color = dialSurface,
                    radius = radius,
                    center = center
                )

                // Fixed top phone heading pointer
                val pointerPath = Path().apply {
                    moveTo(center.x, center.y - radius - 10.dp.toPx())
                    lineTo(center.x - 7.dp.toPx(), center.y - radius + 2.dp.toPx())
                    lineTo(center.x + 7.dp.toPx(), center.y - radius + 2.dp.toPx())
                    close()
                }
                drawPath(pointerPath, color = if (aligned) EzanEmerald else EzanGold)

                // Rotating compass rose + Qibla needle
                rotate(degrees = dialRotation, pivot = center) {
                    for (deg in 0 until 360 step 5) {
                        val isMajor = deg % 30 == 0
                        val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                        val rad = Math.toRadians((deg - 90).toDouble())
                        val outer = Offset(
                            x = center.x + (radius - 6.dp.toPx()) * cos(rad).toFloat(),
                            y = center.y + (radius - 6.dp.toPx()) * sin(rad).toFloat()
                        )
                        val inner = Offset(
                            x = center.x + (radius - 6.dp.toPx() - tickLen) * cos(rad).toFloat(),
                            y = center.y + (radius - 6.dp.toPx() - tickLen) * sin(rad).toFloat()
                        )
                        drawLine(
                            color = if (deg == 0) Color(0xFFD32F2F)
                            else if (isMajor) onSurfaceColor.copy(alpha = 0.7f)
                            else mutedColor.copy(alpha = 0.35f),
                            start = inner,
                            end = outer,
                            strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Turkish cardinal letters: K, D, G, B
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 15.dp.toPx()
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val cardinals = listOf(
                        0 to ("K" to Color(0xFFD32F2F)),
                        90 to ("D" to onSurfaceColor),
                        180 to ("G" to onSurfaceColor),
                        270 to ("B" to onSurfaceColor)
                    )
                    val labelRadius = radius - 32.dp.toPx()
                    for ((deg, pair) in cardinals) {
                        val rad = Math.toRadians((deg - 90).toDouble())
                        val lx = center.x + labelRadius * cos(rad).toFloat()
                        val ly = center.y + labelRadius * sin(rad).toFloat() + 5.dp.toPx()
                        textPaint.color = pair.second.toArgb()
                        drawContext.canvas.nativeCanvas.drawText(pair.first, lx, ly, textPaint)
                    }

                    // Qibla Needle pointing toward Mecca bearing
                    rotate(degrees = bearing, pivot = center) {
                        val needleTop = center.y - (radius - 46.dp.toPx())
                        val needleBottom = center.y + 38.dp.toPx()

                        drawLine(
                            color = if (aligned) EzanEmerald else primaryColor,
                            start = Offset(center.x, needleBottom),
                            end = Offset(center.x, needleTop + 14.dp.toPx()),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Arrow head
                        val arrowPath = Path().apply {
                            moveTo(center.x, needleTop)
                            lineTo(center.x - 10.dp.toPx(), needleTop + 20.dp.toPx())
                            lineTo(center.x + 10.dp.toPx(), needleTop + 20.dp.toPx())
                            close()
                        }
                        drawPath(arrowPath, color = if (aligned) EzanEmerald else primaryColor)

                        // Stylized Kaaba badge near tip
                        val kaabaSize = 22.dp.toPx()
                        val kaabaTopLeft = Offset(
                            center.x - kaabaSize / 2f,
                            center.y - radius + 18.dp.toPx()
                        )
                        drawRect(
                            color = Color(0xFF181818),
                            topLeft = kaabaTopLeft,
                            size = Size(kaabaSize, kaabaSize)
                        )
                        drawRect(
                            color = EzanGold,
                            topLeft = Offset(kaabaTopLeft.x, kaabaTopLeft.y + kaabaSize * 0.22f),
                            size = Size(kaabaSize, kaabaSize * 0.16f)
                        )
                    }

                    // Center pivot cap
                    drawCircle(color = primaryColor, radius = 8.dp.toPx(), center = center)
                    drawCircle(color = EzanGold, radius = 3.5.dp.toPx(), center = center)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Calibration warning if needed
        if (compass.hasSensor && compass.needsCalibration) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = extColors.kerahatContainer,
                border = BorderStroke(1.dp, extColors.kerahatBorder)
            ) {
                Text(
                    text = "Pusulayı kalibre etmek için telefonunuzu havada 8 çizerek hareket ettirin.",
                    color = extColors.kerahatContent,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Info cards: Qibla Angle & Distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Kıble Açısı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(
                            Locale.forLanguageTag("tr-TR"),
                            "%.1f° %s",
                            bearing,
                            QiblaCalculator.cardinalDirectionTr(bearing)
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Kabe Uzaklığı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.qiblaDistanceKm.roundToInt()} km",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
