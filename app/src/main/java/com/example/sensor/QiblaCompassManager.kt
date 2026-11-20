package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object QiblaMath {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LNG = 39.826206

    fun calculateQiblaBearing(lat: Double, lng: Double): Float {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LNG - lng)
        val y = sin(deltaLambda)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLambda)
        val deg = Math.toDegrees(atan2(y, x))
        return ((deg + 360.0) % 360.0).toFloat()
    }

    fun calculateDistanceKm(lat: Double, lng: Double): Int {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - lat)
        val dLon = Math.toRadians(KAABA_LNG - lng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusKm * c).roundToInt()
    }
}

class QiblaCompassSensor(
    context: Context,
    private val onAzimuthChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false

    val isAvailable: Boolean
        get() = rotationVectorSensor != null || (accelerometer != null && magnetometer != null)

    fun start(): Boolean {
        if (!isAvailable) return false
        return if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            val ok1 = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            val ok2 = sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
            ok1 && ok2
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthDeg = ((Math.toDegrees(orientationAngles[0].toDouble()) + 360.0) % 360.0).toFloat()
                onAzimuthChanged(azimuthDeg)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val alpha = 0.85f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                hasGravity = true
                emitFromAccelMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val alpha = 0.85f
                geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                hasMagnetic = true
                emitFromAccelMag()
            }
        }
    }

    private fun emitFromAccelMag() {
        if (!hasGravity || !hasMagnetic) return
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthDeg = ((Math.toDegrees(orientationAngles[0].toDouble()) + 360.0) % 360.0).toFloat()
            onAzimuthChanged(azimuthDeg)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
