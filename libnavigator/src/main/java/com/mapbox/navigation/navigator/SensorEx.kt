package com.mapbox.navigation.navigator

import android.hardware.SensorEvent
import android.os.Build
import android.util.Log
import com.mapbox.navigator.Axes3D
import com.mapbox.navigator.SensorData
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

internal fun SensorEvent.toSensorData(): SensorData? {
    return this.toSensorData3d()
//    return when (this.sensor.type) {
//        Sensor.TYPE_ACCELEROMETER -> this.toSensorData3d()
//        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> this.toSensorData3d()
//        Sensor.TYPE_ORIENTATION -> this.toSensorData3d()
//        Sensor.TYPE_GYROSCOPE -> this.toSensorData3d()
//        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> this.toSensorData3d()
//        Sensor.TYPE_GRAVITY -> this.toSensorData3d()
//        Sensor.TYPE_LINEAR_ACCELERATION -> this.toSensorData3d()
//        Sensor.TYPE_ROTATION_VECTOR -> this.toSensorData3d()
//        Sensor.TYPE_MAGNETIC_FIELD -> this.toSensorData3d()
//        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> this.toSensorData3d()
//        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> this.toSensorData3d()
//        Sensor.TYPE_GAME_ROTATION_VECTOR -> this.toSensorData3d()
//        Sensor.TYPE_SIGNIFICANT_MOTION -> this.toSensorData3d()
//        Sensor.TYPE_POSE_6DOF -> this.toSensorData3d()
//        Sensor.TYPE_STATIONARY_DETECT -> this.toSensorData3d()
//        Sensor.TYPE_MOTION_DETECT -> this.toSensorData3d()
//        Sensor.TYPE_STEP_DETECTOR -> this.toSensorDataScalar()
//        Sensor.TYPE_STEP_COUNTER -> this.toSensorDataScalar()
//        Sensor.TYPE_RELATIVE_HUMIDITY -> this.toSensorDataScalar()
//        Sensor.TYPE_LIGHT -> this.toSensorDataScalar()
//        Sensor.TYPE_PRESSURE -> this.toSensorDataScalar()
//        Sensor.TYPE_PROXIMITY -> this.toSensorDataScalar()
//        Sensor.TYPE_TEMPERATURE -> this.toSensorDataScalar()
//        Sensor.TYPE_AMBIENT_TEMPERATURE -> this.toSensorDataScalar()
//        Sensor.TYPE_HEART_BEAT -> this.toSensorDataScalar()
//        Sensor.TYPE_HEART_RATE -> this.toSensorDataScalar()
//        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> this.toSensorDataScalar()
//        else -> {
//            Log.i("location_debug", "did not transform ${sensor.name} ${sensor.stringType}")
//            null
//        }
//    }
}


fun SensorEvent.toSensorData3d(): SensorData {
    Log.i("location_debug", " emit values ${values.size} ${sensor.name} ${values.joinToString()}")
    val data: Axes3D? = when (values.size) {
        3 -> Axes3D(this.values[0], this.values[1], this.values[2])
        1 -> Axes3D(this.values[0], 0.0f, 0.0f)
        else -> {
            Log.i("location_debug", "Could not transform ${sensor.name} with ${values.size} values as ${values.joinToString()}")
            null
        }
    }
    return SensorData(
            null,
            null,
            this.toTime(),
            data,
            null)
}

fun SensorEvent.toTime(): Date {
    return if (Build.VERSION.SDK_INT < 26) {
        val instantMillis = TimeUnit.NANOSECONDS.toMillis(this.timestamp)
        Date(instantMillis)
    } else {
        val instantTime = this.timestamp
        val instantFullSeconds = instantTime / TimeUnit.SECONDS.toNanos(1)
        val instantNanos = instantTime - instantFullSeconds
        val instantSecs = TimeUnit.NANOSECONDS.toSeconds(instantFullSeconds)
        val instant: Instant = Instant.ofEpochSecond(instantSecs, instantNanos)
        Date.from(instant)
    }
}
