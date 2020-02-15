package com.mapbox.navigation.examples.sensors

import android.app.Application
import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SensorEventViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val turnDegreesLiveData = MutableLiveData<Double>()
    private val turnSensor = TurnSensor()
    private val navigationSensorManager = NavigationSensorManager(sensorManager)

    var eventEmitter: ((SensorEvent) -> Unit) = { }

    init {
        Timber.i("location_debug register sensors")
        navigationSensorManager.start { event ->
            turnSensor.update(event)
            eventEmitter.invoke(event)
        }
    }

    fun observeTurnDegrees(owner: LifecycleOwner, observer: ((Double) -> Unit)) {
        viewModelScope.launch {
            throttleTurnDegrees()
        }
        turnDegreesLiveData.observe(owner, Observer { turnDegrees ->
            observer.invoke(turnDegrees)
            Timber.i("location_debug turnDegrees $turnDegrees")
        })
    }

    private suspend fun throttleTurnDegrees() {
        while(true) {
            turnDegreesLiveData.postValue(turnSensor.turnDegrees())
            delay(TimeUnit.SECONDS.toMillis(1))
        }
    }

    override fun onCleared() {
        Timber.i("location_debug unregister listeners")
        navigationSensorManager.stop()
        eventEmitter = { }

        super.onCleared()
    }
}

