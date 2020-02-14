package com.mapbox.navigation.examples.sensors

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ALL
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import timber.log.Timber

class SensorEventViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorEventListeners = mutableListOf<SensorListener>()

    private val emitter: (SensorEvent) -> Unit = { sensorEvent ->
        externalEmitter.invoke(sensorEvent)
    }
    var externalEmitter : (SensorEvent) -> Unit = { }

    init {
        Timber.i("location_debug register sensors")
        val sensorList = sensorManager.getSensorList(TYPE_ALL).filterNotNull()
        for (sensor in sensorList) {
            Timber.i("location_debug register sensor ${sensor.name}")
            val sensorListener = SensorListener(emitter)
            sensorEventListeners.add(sensorListener)
            sensorManager.registerListener(sensorListener, sensor, 0)
        }
    }

    override fun onCleared() {
        Timber.i("location_debug unregister listeners")
        for (sensorListener in sensorEventListeners) {
            sensorManager.unregisterListener(sensorListener)
        }

        super.onCleared()
    }

    class SensorListener(
        private val emitter: (SensorEvent) -> Unit
    ) : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }

        override fun onSensorChanged(sensorEvent: SensorEvent) {
            emitter.invoke(sensorEvent)
        }
    }
}

// 25 signals per second is 40000 microseconds.
// 1/25 = 0.04 seconds and 40000 microseconds
private fun toSamplingPeriodUs(signalsPerSecond: Int): Int {
    return 1000000 / signalsPerSecond
}