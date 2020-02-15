package com.mapbox.navigation.examples.sensors

import android.hardware.Sensor.TYPE_GRAVITY
import android.hardware.Sensor.TYPE_GYROSCOPE
import android.hardware.SensorEvent
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.sqrt

class TurnSensor {

    private var lastTurnSample = 0.0f
    private var lastTimeNanos = 0L

    private val gravity = FloatArray(3)
    private val gyro = FloatArray(3)

    var turnRadians = 0.0
    fun turnDegrees(): Double {
        return (turnRadians * 180.0 / PI + 360.0) % 360.0
    }

    fun update(event: SensorEvent) {
        when (event.sensor.type) {
            TYPE_GRAVITY -> updateGravity(event)
            TYPE_GYROSCOPE -> updateGyroscope(event)
        }
    }

    private fun updateGravity(sensorEvent: SensorEvent) {
        gravity[0] = sensorEvent.values[0]
        gravity[1] = sensorEvent.values[1]
        gravity[2] = sensorEvent.values[2]
    }

    private fun updateGyroscope(gyroEvent: SensorEvent) {
        val gravityMagnitude = magnitude(gravity)
        if (gravityMagnitude < 0.1) {
            Timber.i("location_debug guarding update gryo")
            // guard against initialization and free falls
            return
        }

        gyro[0] = gyroEvent.values[0]
        gyro[1] = gyroEvent.values[1]
        gyro[2] = gyroEvent.values[2]

        val rotated = dot(gravity, gyro) / gravityMagnitude
        if (lastTimeNanos != 0L) {
            val deltaNanos = gyroEvent.timestamp - lastTimeNanos
            val deltaSeconds = deltaNanos / 1000000000.0
            val integral = 0.5 * deltaSeconds * (rotated + lastTurnSample)
            turnRadians += integral
        }
        lastTimeNanos = gyroEvent.timestamp
        lastTurnSample = rotated
        turnRadians
    }

    private fun dot(gravity: FloatArray, gyro: FloatArray): Float {
        val v1x = gravity[0]
        val v1y = gravity[1]
        val v1z = gravity[2]
        val v2x = gyro[0]
        val v2y = gyro[1]
        val v2z = gyro[2]
        return v1x * v2x + v1y * v2y + v1z * v2z
    }

    private fun magnitude(vector3d: FloatArray): Float {
        val x = vector3d[0]
        val y = vector3d[1]
        val z = vector3d[2]
        return sqrt(x*x + y*y + z*z)
    }

    fun Double.toNormalizedRadians(): Double {
        val twoPi = PI * 2.0
        return (this * PI / 180.0 + twoPi) % twoPi
    }
}
