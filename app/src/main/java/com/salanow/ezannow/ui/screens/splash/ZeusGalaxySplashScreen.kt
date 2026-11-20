package com.salanow.ezannow.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salanow.ezannow.BuildConfig
import com.salanow.ezannow.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class StarSpec(
    val xFrac: Float,
    val yFrac: Float,
    val radiusDp: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val phase: Float,
    val color: Color
)

private data class SpiralParticle(
    val armIndex: Int,
    val distanceFrac: Float,
    val angleOffset: Float,
    val sizeDp: Float,
    val alpha: Float,
    val color: Color
)

@Composable
fun ZeusGalaxySplashScreen(
    onFinished: () -> Unit
) {
    val stars = remember {
        val rng = Random(42)
        val palette = listOf(
            Color.White,
            Color(0xFFBDE8FF),
            Color(0xFFFFE7A3),
            Color(0xFFD1B3FF)
        )
        List(125) {
            StarSpec(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                radiusDp = 0.8f + rng.nextFloat() * 1.9f,
                baseAlpha = 0.35f + rng.nextFloat() * 0.65f,
                twinkleSpeed = 2f + rng.nextFloat() * 4f,
                phase = rng.nextFloat() * 6.28f,
                color = palette[rng.nextInt(palette.size)]
            )
        }
    }

    val spiralParticles = remember {
        val rng = Random(137)
        val armColors = listOf(
            Color(0xFF3A2F9B),
            Color(0xFF7A3CE8),
            Color(0xFF29C7E8),
            Color(0xFFF5C451)
        )
        List(220) { idx ->
            val arm = idx % 4
            val dist = 0.08f + rng.nextFloat() * 0.92f
            SpiralParticle(
                armIndex = arm,
                distanceFrac = dist,
                angleOffset = (rng.nextFloat() - 0.5f) * 0.55f,
                sizeDp = 1.1f + (1f - dist) * 2.4f + rng.nextFloat() * 1.4f,
                alpha = (0.25f + (1f - dist * 0.6f) * 0.55f).coerceIn(0.15f, 0.9f),
                color = armColors[(arm + rng.nextInt(2)) % armColors.size]
            )
        }
    }

    val galaxySpin = remember { Animatable(0f) }
    val nebulaReveal = remember { Animatable(0f) }
    val zeusAlpha = remember { Animatable(0f) }
    val zeusScale = remember { Animatable(0.80f) }
    val zeusTilt = remember { Animatable(0f) }
    val boltProgress = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val shockwave = remember { Animatable(0f) }
    val shakeIntensity = remember { Animatable(0f) }
    val brandReveal = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // Continuous slow galaxy spin + twinkle
            launch {
                galaxySpin.animateTo(
                    targetValue = 140f,
                    animationSpec = tween(durationMillis = 3600, easing = LinearEasing)
                )
            }
            // Phase 1 & 2: Nebula expansion
            launch {
                delay(180)
                nebulaReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                )
            }
            // Phase 2: Zeus materializing inside the nebula
            launch {
                delay(480)
                launch {
                    zeusAlpha.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
                }
                launch {
                    zeusScale.animateTo(1.04f, tween(1150, easing = FastOutSlowInEasing))
                }
            }
            // Phase 3: Lightning surge & throw from Zeus's raised fist
            launch {
                delay(1380)
                zeusTilt.animateTo(-2.6f, tween(180, easing = FastOutSlowInEasing))
                launch {
                    zeusTilt.animateTo(1.6f, tween(200, easing = FastOutSlowInEasing))
                }
                launch {
                    boltProgress.animateTo(1f, tween(220, easing = LinearEasing))
                }
                delay(110)
                launch {
                    flashAlpha.snapTo(0.60f)
                    flashAlpha.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
                }
                launch {
                    shakeIntensity.snapTo(1f)
                    shakeIntensity.animateTo(0f, tween(380, easing = LinearEasing))
                }
                launch {
                    shockwave.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
                }
                zeusTilt.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            }
            // Phase 4: Brand reveal & completion
            launch {
                delay(2100)
                brandReveal.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                delay(3500)
                onFinished()
            }
        }
    }

    val spinDeg = galaxySpin.value
    val shakeAmp = shakeIntensity.value
    val shakeX = if (shakeAmp > 0f) (sin(spinDeg * 1.9f) * 7f * shakeAmp) else 0f
    val shakeY = if (shakeAmp > 0f) (cos(spinDeg * 2.4f) * 6f * shakeAmp) else 0f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
    ) {
        val density = LocalDensity.current
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }

        // Size & position of Zeus portrait (3:4 aspect ratio) centered in upper-middle nebula
        val zeusW = (screenW * 0.84f).coerceAtMost(screenH * 0.45f)
        val zeusH = zeusW * (960f / 720f)
        val zeusWdp = with(density) { zeusW.toDp() }
        val zeusHdp = with(density) { zeusH.toDp() }
        val zeusCenterY = screenH * 0.37f
        val zeusTopPx = zeusCenterY - zeusH / 2f
        val zeusLeftPx = (screenW - zeusW) / 2f
        val floatPx = sin(spinDeg * 0.11f) * with(density) { 4.dp.toPx() }

        // Back Layer: Deep Cosmic Sky + Twinkling Stars + Rotating Galaxy + Back Nebula
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(shakeX.roundToInt(), shakeY.roundToInt()) }
        ) {
            val w = size.width
            val h = size.height
            val galaxyCenter = Offset(w * 0.5f, zeusCenterY)
            val galaxyRadius = minOf(w, h) * 0.52f

            // 1. Deep Cosmic Sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03040A),
                        Color(0xFF090B1E),
                        Color(0xFF040611)
                    )
                )
            )

            // 2. Twinkling Starfield
            stars.forEach { star ->
                val twinkle = 0.55f + 0.45f * sin(spinDeg * 0.08f * star.twinkleSpeed + star.phase)
                drawCircle(
                    color = star.color.copy(alpha = (star.baseAlpha * twinkle).coerceIn(0.08f, 1f)),
                    radius = star.radiusDp.dp.toPx(),
                    center = Offset(star.xFrac * w, star.yFrac * h)
                )
            }

            // 3. Rotating Spiral Galaxy
            val galAlpha = (0.38f + 0.62f * (spinDeg / 25f).coerceIn(0f, 1f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x667A3CE8),
                        Color(0x333A2F9B),
                        Color.Transparent
                    ),
                    center = galaxyCenter,
                    radius = galaxyRadius * 1.25f
                ),
                radius = galaxyRadius * 1.25f,
                center = galaxyCenter
            )

            rotate(degrees = spinDeg * 0.65f, pivot = galaxyCenter) {
                spiralParticles.forEach { p ->
                    val baseArmAngle = p.armIndex * (PI.toFloat() / 2f)
                    val twist = p.distanceFrac * 3.1f
                    val theta = baseArmAngle + twist + p.angleOffset
                    val r = p.distanceFrac * galaxyRadius
                    val px = galaxyCenter.x + r * cos(theta)
                    val py = galaxyCenter.y + r * sin(theta) * 0.74f
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha * galAlpha),
                        radius = p.sizeDp.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // Galactic core & Zeus halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xDD8CEBFF),
                        Color(0x887A3CE8),
                        Color(0x333A2F9B),
                        Color.Transparent
                    ),
                    center = galaxyCenter,
                    radius = galaxyRadius * 0.60f
                ),
                radius = galaxyRadius * 0.60f,
                center = galaxyCenter
            )

            // 4. Layered Cosmic Nebula Clouds
            val neb = nebulaReveal.value
            if (neb > 0f) {
                val nebScale = 0.58f + 0.52f * neb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x888E2DE2).copy(alpha = 0.58f * neb),
                            Color(0x444A00E0).copy(alpha = 0.35f * neb),
                            Color.Transparent
                        ),
                        center = Offset(galaxyCenter.x - w * 0.16f, galaxyCenter.y - h * 0.04f),
                        radius = galaxyRadius * 1.15f * nebScale
                    ),
                    radius = galaxyRadius * 1.15f * nebScale,
                    center = Offset(galaxyCenter.x - w * 0.16f, galaxyCenter.y - h * 0.04f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x7700D2FF).copy(alpha = 0.52f * neb),
                            Color(0x33146C43).copy(alpha = 0.35f * neb),
                            Color.Transparent
                        ),
                        center = Offset(galaxyCenter.x + w * 0.16f, galaxyCenter.y + h * 0.04f),
                        radius = galaxyRadius * 1.05f * nebScale
                    ),
                    radius = galaxyRadius * 1.05f * nebScale,
                    center = Offset(galaxyCenter.x + w * 0.16f, galaxyCenter.y + h * 0.04f)
                )
            }
        }

        // Middle Layer: Illustrated Mythological Zeus emerging from the Nebula
        val zAlpha = zeusAlpha.value
        if (zAlpha > 0.01f) {
            Image(
                painter = painterResource(id = R.drawable.zeus_nebula),
                contentDescription = "Zeus",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (zeusLeftPx + shakeX).roundToInt(),
                            (zeusTopPx + floatPx + shakeY).roundToInt()
                        )
                    }
                    .size(width = zeusWdp, height = zeusHdp)
                    .graphicsLayer {
                        alpha = zAlpha
                        scaleX = zeusScale.value
                        scaleY = zeusScale.value
                        rotationZ = zeusTilt.value
                    }
            )
        }

        // Front Layer: Live Eye Arcs, Raised-Fist Energy, Thrown Multi-Branch Lightning, Foreground Mist & Shockwave
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(shakeX.roundToInt(), shakeY.roundToInt()) }
        ) {
            val w = size.width
            val h = size.height
            val impactPoint = Offset(w * 0.5f, h * 0.77f)

            // Map Zeus's eyes and raised fist into screen space matching current scale
            fun zeusPoint(nx: Float, ny: Float): Offset {
                val cx = zeusLeftPx + zeusW * 0.5f
                val cy = zeusTopPx + zeusH * 0.5f + floatPx
                val dx = (nx - 0.5f) * zeusW * zeusScale.value
                val dy = (ny - 0.5f) * zeusH * zeusScale.value
                return Offset(cx + dx, cy + dy)
            }

            val leftEye = zeusPoint(0.475f, 0.175f)
            val rightEye = zeusPoint(0.545f, 0.175f)
            val raisedFist = zeusPoint(0.90f, 0.39f)

            // Foreground cosmic nebula mist at Zeus's lower waist so he blends into the cosmos
            val neb = nebulaReveal.value
            if (neb > 0f && zAlpha > 0.05f) {
                val mistCenter = zeusPoint(0.5f, 0.90f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x993A1D78).copy(alpha = 0.55f * zAlpha),
                            Color(0x5500A8CC).copy(alpha = 0.30f * zAlpha),
                            Color.Transparent
                        ),
                        center = mistCenter,
                        radius = zeusW * 0.62f
                    ),
                    radius = zeusW * 0.62f,
                    center = mistCenter
                )
            }

            // Crackling eye-lightning arcs & fist energy charge
            if (zAlpha > 0.35f) {
                val flicker = 0.55f + 0.45f * sin(spinDeg * 0.55f)
                val chargeAlpha = zAlpha * flicker

                // Eye glow + flickering miniature eye sparks
                listOf(leftEye, rightEye).forEach { eye ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF).copy(alpha = 0.85f * chargeAlpha),
                                Color(0xFF00E5FF).copy(alpha = 0.50f * chargeAlpha),
                                Color.Transparent
                            ),
                            center = eye,
                            radius = 16.dp.toPx()
                        ),
                        radius = 16.dp.toPx(),
                        center = eye
                    )
                }

                // Dynamic energy sphere around Zeus's raised fist before/during bolt release
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f * chargeAlpha),
                            Color(0xFF00E5FF).copy(alpha = 0.72f * chargeAlpha),
                            Color.Transparent
                        ),
                        center = raisedFist,
                        radius = 28.dp.toPx()
                    ),
                    radius = 28.dp.toPx(),
                    center = raisedFist
                )
            }

            // Multi-branched jagged lightning bolt hurled from raised fist to impact point
            val bolt = boltProgress.value
            if (bolt > 0f) {
                val end = Offset(
                    raisedFist.x + (impactPoint.x - raisedFist.x) * bolt,
                    raisedFist.y + (impactPoint.y - raisedFist.y) * bolt
                )
                drawMultiBranchLightning(
                    start = raisedFist,
                    end = end,
                    progress = bolt,
                    fadeOut = (1f - brandReveal.value * 0.52f).coerceIn(0.35f, 1f)
                )
            }

            // Shockwave ring at impact point
            val sw = shockwave.value
            if (sw > 0f) {
                val ringRadius = (18f + sw * minOf(w, h) * 0.34f).dp.toPx()
                val ringAlpha = (1f - sw).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFFF5C451).copy(alpha = ringAlpha * 0.88f),
                    radius = ringRadius,
                    center = impactPoint,
                    style = Stroke(width = (5.dp.toPx() * (1f - sw * 0.7f)).coerceAtLeast(1f))
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = ringAlpha * 0.65f),
                    radius = ringRadius * 0.72f,
                    center = impactPoint,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Emerald-gold brand aura
            val br = brandReveal.value
            if (br > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xAA198754).copy(alpha = 0.68f * br),
                            Color(0x55F5C451).copy(alpha = 0.42f * br),
                            Color.Transparent
                        ),
                        center = impactPoint,
                        radius = 155.dp.toPx()
                    ),
                    radius = 155.dp.toPx(),
                    center = impactPoint
                )
            }

            // Flash overlay on bolt strike
            val flash = flashAlpha.value
            if (flash > 0f) {
                drawRect(color = Color(0xFFD8F7FF).copy(alpha = flash))
            }
        }

        // Brand Reveal Text
        val br = brandReveal.value
        if (br > 0.02f) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 86.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "EzanNow!",
                    color = Color.White.copy(alpha = br),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Namaz Vakitleri • v${BuildConfig.VERSION_NAME}",
                    color = Color(0xFFF5C451).copy(alpha = br),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Skip Pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.14f)
        ) {
            Text(
                text = "Atla",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

private fun DrawScope.drawMultiBranchLightning(
    start: Offset,
    end: Offset,
    progress: Float,
    fadeOut: Float
) {
    val mainPts = listOf(
        Offset(0f, 0f),
        Offset(-0.14f, 0.16f),
        Offset(0.11f, 0.30f),
        Offset(-0.18f, 0.48f),
        Offset(0.12f, 0.64f),
        Offset(-0.08f, 0.82f),
        Offset(0f, 1.0f)
    )

    val dx = end.x - start.x
    val dy = end.y - start.y
    val spanX = 76.dp.toPx()

    val resolvedMain = mainPts.map { p ->
        Offset(
            x = start.x + dx * p.y + p.x * spanX,
            y = start.y + dy * p.y
        )
    }

    val mainPath = Path().apply {
        moveTo(resolvedMain.first().x, resolvedMain.first().y)
        for (i in 1 until resolvedMain.size) {
            lineTo(resolvedMain[i].x, resolvedMain[i].y)
        }
    }

    val branchPaths = buildList {
        if (progress > 0.35f) {
            val b1Start = resolvedMain[2]
            add(Path().apply {
                moveTo(b1Start.x, b1Start.y)
                lineTo(b1Start.x + 34.dp.toPx(), b1Start.y + 30.dp.toPx())
                lineTo(b1Start.x + 20.dp.toPx(), b1Start.y + 56.dp.toPx())
                lineTo(b1Start.x + 44.dp.toPx(), b1Start.y + 80.dp.toPx())
            })
        }
        if (progress > 0.55f) {
            val b2Start = resolvedMain[3]
            add(Path().apply {
                moveTo(b2Start.x, b2Start.y)
                lineTo(b2Start.x - 40.dp.toPx(), b2Start.y + 28.dp.toPx())
                lineTo(b2Start.x - 26.dp.toPx(), b2Start.y + 54.dp.toPx())
                lineTo(b2Start.x - 50.dp.toPx(), b2Start.y + 78.dp.toPx())
            })
        }
        if (progress > 0.72f) {
            val b3Start = resolvedMain[4]
            add(Path().apply {
                moveTo(b3Start.x, b3Start.y)
                lineTo(b3Start.x + 28.dp.toPx(), b3Start.y + 22.dp.toPx())
                lineTo(b3Start.x + 38.dp.toPx(), b3Start.y + 46.dp.toPx())
            })
        }
    }

    drawPath(
        path = mainPath,
        color = Color(0xFF00E5FF).copy(alpha = 0.32f * fadeOut),
        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = mainPath,
        color = Color(0xFF7DF9FF).copy(alpha = 0.80f * fadeOut),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = mainPath,
        color = Color(0xFFFFFCE0).copy(alpha = fadeOut),
        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    branchPaths.forEach { bPath ->
        drawPath(
            path = bPath,
            color = Color(0xFF00E5FF).copy(alpha = 0.38f * fadeOut),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = bPath,
            color = Color.White.copy(alpha = 0.90f * fadeOut),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
