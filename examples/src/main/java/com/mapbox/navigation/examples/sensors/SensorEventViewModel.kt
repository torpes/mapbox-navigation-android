package com.mapbox.navigation.examples.sensors

import android.app.Application
import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import timber.log.Timber

class SensorEventViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val navigationSensorManager = NavigationSensorManager(sensorManager)

    var eventEmitter: ((SensorEvent) -> Unit) = { }

    init {
        Timber.i("location_debug register sensors")
        navigationSensorManager.start { event ->
            eventEmitter.invoke(event)
        }
    }

    override fun onCleared() {
        Timber.i("location_debug unregister listeners")
        navigationSensorManager.stop()
        eventEmitter = { }

        super.onCleared()
    }
}
