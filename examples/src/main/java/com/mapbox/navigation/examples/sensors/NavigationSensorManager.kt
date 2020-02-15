package com.mapbox.navigation.examples.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import timber.log.Timber

class NavigationSensorManager(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private var eventEmitter: (SensorEvent) -> Unit = { }

    fun start(eventEmitter: (SensorEvent) -> Unit) {
        this.eventEmitter = eventEmitter
        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL).filterNotNull()
        for (sensor in sensorList) {
            Timber.i("location_debug register sensor $sensor ")
            sensorManager.registerListener(this, sensor, toSamplingPeriodUs(10))
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        eventEmitter.invoke(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Haven't found a need for this
    }

    /**
     * Helper function to turn signalsPerSecond into what Android expects, samplingPeriodUs
     *
     * 25 signals per second is 40000 samplingPeriodUs.
     */
    private fun toSamplingPeriodUs(signalsPerSecond: Int): Int {
        return 1000000 / signalsPerSecond
    }
}
