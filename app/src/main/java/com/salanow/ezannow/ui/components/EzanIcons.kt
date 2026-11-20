package com.salanow.ezannow.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.salanow.ezannow.domain.model.PrayerType

object EzanIcons {

    fun forPrayer(type: PrayerType): ImageVector = when (type) {
        PrayerType.IMSAK -> ImsakMoon
        PrayerType.GUNES -> Sunrise
        PrayerType.OGLE -> NoonSun
        PrayerType.IKINDI -> AfternoonSun
        PrayerType.AKSAM -> Sunset
        PrayerType.YATSI -> IshaNight
    }

    val Mosque: ImageVector by lazy {
        vector("Mosque") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                curveTo(10.5f, 4.5f, 7f, 7f, 7f, 10.5f)
                verticalLineTo(20f)
                horizontalLineTo(10.5f)
                verticalLineTo(16f)
                curveTo(10.5f, 15.17f, 11.17f, 14.5f, 12f, 14.5f)
                curveTo(12.83f, 14.5f, 13.5f, 15.17f, 13.5f, 16f)
                verticalLineTo(20f)
                horizontalLineTo(17f)
                verticalLineTo(10.5f)
                curveTo(17f, 7f, 13.5f, 4.5f, 12f, 3f)
                close()
                moveTo(3f, 11f)
                verticalLineTo(20f)
                horizontalLineTo(5.5f)
                verticalLineTo(11f)
                lineTo(4.25f, 8f)
                lineTo(3f, 11f)
                close()
                moveTo(18.5f, 11f)
                verticalLineTo(20f)
                horizontalLineTo(21f)
                verticalLineTo(11f)
                lineTo(19.75f, 8f)
                lineTo(18.5f, 11f)
                close()
            }
        }
    }

    val CalendarTable: ImageVector by lazy {
        vector("CalendarTable") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 10.5f)
                horizontalLineTo(20f)
                moveTo(8f, 3.5f)
                verticalLineTo(6f)
                moveTo(16f, 3.5f)
                verticalLineTo(6f)
                moveTo(9.5f, 10.5f)
                verticalLineTo(20f)
                moveTo(14.5f, 10.5f)
                verticalLineTo(20f)
                moveTo(4f, 15.2f)
                horizontalLineTo(20f)
            }
        }
    }

    val Compass: ImageVector by lazy {
        vector("Compass") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
                curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.6f, 8.4f)
                lineTo(13.4f, 13.4f)
                lineTo(8.4f, 15.6f)
                lineTo(10.6f, 10.6f)
                close()
            }
        }
    }

    val Tasbih: ImageVector by lazy {
        vector("Tasbih") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 20f)
                curveTo(16.4f, 20f, 19.5f, 16.8f, 19.5f, 12.2f)
                curveTo(19.5f, 7.6f, 16.2f, 4.2f, 12f, 4.2f)
                curveTo(7.8f, 4.2f, 4.5f, 7.6f, 4.5f, 12.2f)
                curveTo(4.5f, 16.8f, 7.6f, 20f, 12f, 20f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2.5f)
                lineTo(13.6f, 5.5f)
                lineTo(10.4f, 5.5f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 12.2f)
                moveToRelative(-2f, 0f)
                arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
                arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
            }
        }
    }

    val SettingsGear: ImageVector by lazy {
        vector("SettingsGear") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
                curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                close()
                moveTo(19.4f, 15f)
                lineTo(21f, 13.6f)
                verticalLineTo(10.4f)
                lineTo(19.4f, 9f)
                lineTo(18.6f, 7f)
                lineTo(19.2f, 5f)
                lineTo(16.9f, 2.7f)
                lineTo(14.9f, 3.3f)
                lineTo(13f, 2.5f)
                lineTo(12f, 2f)
                lineTo(11f, 2.5f)
                lineTo(9.1f, 3.3f)
                lineTo(7.1f, 2.7f)
                lineTo(4.8f, 5f)
                lineTo(5.4f, 7f)
                lineTo(4.6f, 9f)
                lineTo(3f, 10.4f)
                verticalLineTo(13.6f)
                lineTo(4.6f, 15f)
                lineTo(5.4f, 17f)
                lineTo(4.8f, 19f)
                lineTo(7.1f, 21.3f)
                lineTo(9.1f, 20.7f)
                lineTo(11f, 21.5f)
                lineTo(12f, 22f)
                lineTo(13f, 21.5f)
                lineTo(14.9f, 20.7f)
                lineTo(16.9f, 21.3f)
                lineTo(19.2f, 19f)
                lineTo(18.6f, 17f)
                close()
            }
        }
    }

    val LocationPin: ImageVector by lazy {
        vector("LocationPin") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2.5f)
                curveTo(8.41f, 2.5f, 5.5f, 5.41f, 5.5f, 9f)
                curveTo(5.5f, 13.88f, 12f, 21.5f, 12f, 21.5f)
                curveTo(12f, 21.5f, 18.5f, 13.88f, 18.5f, 9f)
                curveTo(18.5f, 5.41f, 15.59f, 2.5f, 12f, 2.5f)
                close()
                moveTo(12f, 11.5f)
                curveTo(10.62f, 11.5f, 9.5f, 10.38f, 9.5f, 9f)
                curveTo(9.5f, 7.62f, 10.62f, 6.5f, 12f, 6.5f)
                curveTo(13.38f, 6.5f, 14.5f, 7.62f, 14.5f, 9f)
                curveTo(14.5f, 10.38f, 13.38f, 11.5f, 12f, 11.5f)
                close()
            }
        }
    }

    val MyLocationGps: ImageVector by lazy {
        vector("MyLocationGps") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 19f)
                curveTo(15.87f, 19f, 19f, 15.87f, 19f, 12f)
                curveTo(19f, 8.13f, 15.87f, 5f, 12f, 5f)
                curveTo(8.13f, 5f, 5f, 8.13f, 5f, 12f)
                curveTo(5f, 15.87f, 8.13f, 19f, 12f, 19f)
                close()
                moveTo(12f, 2.5f)
                verticalLineTo(5f)
                moveTo(12f, 19f)
                verticalLineTo(21.5f)
                moveTo(2.5f, 12f)
                horizontalLineTo(5f)
                moveTo(19f, 12f)
                horizontalLineTo(21.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 12f)
                moveToRelative(-2.5f, 0f)
                arcToRelative(2.5f, 2.5f, 0f, true, true, 5f, 0f)
                arcToRelative(2.5f, 2.5f, 0f, true, true, -5f, 0f)
            }
        }
    }

    val BellOn: ImageVector by lazy {
        vector("BellOn") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 21.5f)
                curveTo(13.1f, 21.5f, 14f, 20.6f, 14f, 19.5f)
                horizontalLineTo(10f)
                curveTo(10f, 20.6f, 10.9f, 21.5f, 12f, 21.5f)
                close()
                moveTo(18f, 15.5f)
                verticalLineTo(10.5f)
                curveTo(18f, 7.43f, 16.37f, 4.86f, 13.5f, 4.18f)
                verticalLineTo(3.5f)
                curveTo(13.5f, 2.67f, 12.83f, 2f, 12f, 2f)
                curveTo(11.17f, 2f, 10.5f, 2.67f, 10.5f, 3.5f)
                verticalLineTo(4.18f)
                curveTo(7.64f, 4.86f, 6f, 7.42f, 6f, 10.5f)
                verticalLineTo(15.5f)
                lineTo(4f, 17.5f)
                verticalLineTo(18.5f)
                horizontalLineTo(20f)
                verticalLineTo(17.5f)
                close()
            }
        }
    }

    val BellOff: ImageVector by lazy {
        vector("BellOff") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 5.2f)
                curveTo(9.9f, 4.5f, 10.9f, 4.2f, 12f, 4.2f)
                curveTo(15.3f, 4.2f, 17.5f, 6.7f, 17.5f, 10.2f)
                verticalLineTo(13.5f)
                moveTo(6.5f, 10.5f)
                verticalLineTo(15.5f)
                lineTo(4.5f, 17.5f)
                horizontalLineTo(16.5f)
                moveTo(10f, 19.8f)
                curveTo(10.4f, 20.6f, 11.1f, 21f, 12f, 21f)
                curveTo(12.9f, 21f, 13.6f, 20.6f, 14f, 19.8f)
                moveTo(3.5f, 3.5f)
                lineTo(20.5f, 20.5f)
            }
        }
    }

    val KerahatWarning: ImageVector by lazy {
        vector("KerahatWarning") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.5f)
                lineTo(21f, 19.5f)
                horizontalLineTo(3f)
                close()
                moveTo(12f, 10f)
                verticalLineTo(14f)
                moveTo(12f, 16.8f)
                verticalLineTo(17.2f)
            }
        }
    }

    val ChevronDown: ImageVector by lazy {
        vector("ChevronDown") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }
    }

    val ChevronLeft: ImageVector by lazy {
        vector("ChevronLeft") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 6f)
                lineTo(9f, 12f)
                lineTo(15f, 18f)
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        vector("ChevronRight") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 6f)
                lineTo(15f, 12f)
                lineTo(9f, 18f)
            }
        }
    }

    val Search: ImageVector by lazy {
        vector("Search") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10.5f, 17f)
                curveTo(14.09f, 17f, 17f, 14.09f, 17f, 10.5f)
                curveTo(17f, 6.91f, 14.09f, 4f, 10.5f, 4f)
                curveTo(6.91f, 4f, 4f, 6.91f, 4f, 10.5f)
                curveTo(4f, 14.09f, 6.91f, 17f, 10.5f, 17f)
                close()
                moveTo(15.5f, 15.5f)
                lineTo(20f, 20f)
            }
        }
    }

    val Close: ImageVector by lazy {
        vector("Close") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    val Add: ImageVector by lazy {
        vector("Add") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }
    }

    val Check: ImageVector by lazy {
        vector("Check") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12.5f)
                lineTo(9.5f, 17f)
                lineTo(19f, 7f)
            }
        }
    }

    val Refresh: ImageVector by lazy {
        vector("Refresh") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.5f, 12f)
                curveTo(19.5f, 16.14f, 16.14f, 19.5f, 12f, 19.5f)
                curveTo(7.86f, 19.5f, 4.5f, 16.14f, 4.5f, 12f)
                curveTo(4.5f, 7.86f, 7.86f, 4.5f, 12f, 4.5f)
                curveTo(14.7f, 4.5f, 17.1f, 5.9f, 18.4f, 8.1f)
                moveTo(19.5f, 4.5f)
                verticalLineTo(8.5f)
                horizontalLineTo(15.5f)
            }
        }
    }

    val Trash: ImageVector by lazy {
        vector("Trash") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 6.5f)
                horizontalLineTo(20f)
                moveTo(9f, 6.5f)
                verticalLineTo(4.5f)
                horizontalLineTo(15f)
                verticalLineTo(6.5f)
                moveTo(6.5f, 6.5f)
                lineTo(7.5f, 19.5f)
                horizontalLineTo(16.5f)
                lineTo(17.5f, 6.5f)
            }
        }
    }

    val ImsakMoon: ImageVector by lazy {
        vector("ImsakMoon") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.5f, 4f)
                curveTo(9.1f, 4.5f, 6f, 8.3f, 6.4f, 12.7f)
                curveTo(6.8f, 16.7f, 10.2f, 19.8f, 14.3f, 19.8f)
                curveTo(16.2f, 19.8f, 18f, 19.1f, 19.3f, 17.9f)
                curveTo(15.6f, 18.1f, 12.4f, 15.3f, 12.1f, 11.5f)
                curveTo(11.9f, 8.6f, 13.5f, 5.8f, 16f, 4.4f)
                curveTo(15.2f, 4.1f, 14.3f, 3.9f, 13.5f, 4f)
                close()
            }
        }
    }

    val Sunrise: ImageVector by lazy {
        vector("Sunrise") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.5f, 18f)
                horizontalLineTo(20.5f)
                moveTo(7f, 18f)
                curveTo(7f, 15.2f, 9.2f, 13f, 12f, 13f)
                curveTo(14.8f, 13f, 17f, 15.2f, 17f, 18f)
                moveTo(12f, 4.5f)
                verticalLineTo(9.5f)
                moveTo(9.5f, 7f)
                lineTo(12f, 4.5f)
                lineTo(14.5f, 7f)
                moveTo(5.2f, 11.2f)
                lineTo(6.8f, 12.8f)
                moveTo(18.8f, 11.2f)
                lineTo(17.2f, 12.8f)
            }
        }
    }

    val NoonSun: ImageVector by lazy {
        vector("NoonSun") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 16.2f)
                curveTo(14.32f, 16.2f, 16.2f, 14.32f, 16.2f, 12f)
                curveTo(16.2f, 9.68f, 14.32f, 7.8f, 12f, 7.8f)
                curveTo(9.68f, 7.8f, 7.8f, 9.68f, 7.8f, 12f)
                curveTo(7.8f, 14.32f, 9.68f, 16.2f, 12f, 16.2f)
                close()
                moveTo(12f, 3f)
                verticalLineTo(5f)
                moveTo(12f, 19f)
                verticalLineTo(21f)
                moveTo(3f, 12f)
                horizontalLineTo(5f)
                moveTo(19f, 12f)
                horizontalLineTo(21f)
                moveTo(5.6f, 5.6f)
                lineTo(7.1f, 7.1f)
                moveTo(16.9f, 16.9f)
                lineTo(18.4f, 18.4f)
                moveTo(18.4f, 5.6f)
                lineTo(16.9f, 7.1f)
                moveTo(7.1f, 16.9f)
                lineTo(5.6f, 18.4f)
            }
        }
    }

    val AfternoonSun: ImageVector by lazy {
        vector("AfternoonSun") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10f, 15.5f)
                curveTo(12.2f, 15.5f, 14f, 13.7f, 14f, 11.5f)
                curveTo(14f, 9.3f, 12.2f, 7.5f, 10f, 7.5f)
                curveTo(7.8f, 7.5f, 6f, 9.3f, 6f, 11.5f)
                curveTo(6f, 13.7f, 7.8f, 15.5f, 10f, 15.5f)
                close()
                moveTo(10f, 4f)
                verticalLineTo(5.5f)
                moveTo(4f, 11.5f)
                horizontalLineTo(2.5f)
                moveTo(5.5f, 6.8f)
                lineTo(4.4f, 5.7f)
                moveTo(13f, 18f)
                horizontalLineTo(21f)
                moveTo(15f, 14.5f)
                horizontalLineTo(19.5f)
            }
        }
    }

    val Sunset: ImageVector by lazy {
        vector("Sunset") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.5f, 18f)
                horizontalLineTo(20.5f)
                moveTo(7f, 18f)
                curveTo(7f, 15.2f, 9.2f, 13f, 12f, 13f)
                curveTo(14.8f, 13f, 17f, 15.2f, 17f, 18f)
                moveTo(12f, 4.5f)
                verticalLineTo(9.5f)
                moveTo(9.5f, 7.5f)
                lineTo(12f, 10f)
                lineTo(14.5f, 7.5f)
                moveTo(5.2f, 11.5f)
                lineTo(6.8f, 13f)
                moveTo(18.8f, 11.5f)
                lineTo(17.2f, 13f)
            }
        }
    }

    val IshaNight: ImageVector by lazy {
        vector("IshaNight") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.2f, 4.5f)
                curveTo(8.3f, 5f, 5.6f, 8.6f, 6.2f, 12.6f)
                curveTo(6.8f, 16.5f, 10.5f, 19.2f, 14.4f, 18.6f)
                curveTo(16f, 18.3f, 17.3f, 17.5f, 18.3f, 16.4f)
                curveTo(15.2f, 16.8f, 12.3f, 14.6f, 11.8f, 11.4f)
                curveTo(11.4f, 8.9f, 12.6f, 6.4f, 14.7f, 5.1f)
                curveTo(13.9f, 4.6f, 13.1f, 4.4f, 12.2f, 4.5f)
                close()
                moveTo(18.2f, 5.5f)
                lineTo(18.8f, 7.1f)
                lineTo(20.5f, 7.7f)
                lineTo(18.8f, 8.3f)
                lineTo(18.2f, 10f)
                lineTo(17.6f, 8.3f)
                lineTo(15.9f, 7.7f)
                lineTo(17.6f, 7.1f)
                close()
            }
        }
    }

    private fun vector(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()
    }
}
