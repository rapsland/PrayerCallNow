package com.salanow.ezannow.ui.screens.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.salanow.ezannow.domain.calculator.QiblaCalculator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassReading(
    val trueHeadingDegrees: Float = 0f,
    val hasSensor: Boolean = true,
    val needsCalibration: Boolean = false
)

@Composable
fun rememberCompassReading(
    latitude: Double,
    longitude: Double
): State<CompassReading> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(CompassReading()) }

    DisposableEffect(context, latitude, longitude) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            state.value = CompassReading(hasSensor = false)
            return@DisposableEffect onDispose {}
        }

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationVectorSensor == null && (accelerometer == null || magnetometer == null)) {
            state.value = CompassReading(hasSensor = false)
            return@DisposableEffect onDispose {}
        }

        val declinationDeg = runCatching {
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination
        }.getOrDefault(0f)

        var smoothedSin = 0.0
        var smoothedCos = 1.0
        var initialized = false
        var needsCalib = false

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val accelData = FloatArray(3)
        val magData = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        fun pushMagneticAzimuth(azimuthDeg: Float) {
            val trueNorth = QiblaCalculator.normalize360(azimuthDeg + declinationDeg)
            val rad = Math.toRadians(trueNorth.toDouble())
            val alpha = 0.16
            if (!initialized) {
                smoothedSin = sin(rad)
                smoothedCos = cos(rad)
                initialized = true
            } else {
                smoothedSin = smoothedSin + alpha * (sin(rad) - smoothedSin)
                smoothedCos = smoothedCos + alpha * (cos(rad) - smoothedCos)
            }
            val filtered = QiblaCalculator.normalize360(
                Math.toDegrees(atan2(smoothedSin, smoothedCos)).toFloat()
            )
            state.value = CompassReading(
                trueHeadingDegrees = filtered,
                hasSensor = true,
                needsCalibration = needsCalib
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        val deg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        pushMagneticAzimuth(deg)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelData, 0, 3)
                        hasAccel = true
                        if (hasMag && SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magData)) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val deg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            pushMagneticAzimuth(deg)
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magData, 0, 3)
                        hasMag = true
                        if (hasAccel && SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magData)) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            val deg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            pushMagneticAzimuth(deg)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    needsCalib = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                    state.value = state.value.copy(needsCalibration = needsCalib)
                }
            }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return state
}
