package com.mapbox.navigation.core.telemetry

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.Event
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.BuildConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.metrics.MetricEvent
import com.mapbox.navigation.core.metrics.MetricsReporter
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigation.utils.exceptions.NavigationException
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.ifChannelException
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import com.mapbox.navigation.utils.time.Time
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.jetbrains.annotations.TestOnly

private enum class LocationBufferCommands {
    BUFFER_ADD,
    BUFFER_REMOVE,
    BUFFER_CLEAR,
    BUFFER_READ,
}

/**
 * This data class defines an action to be taken against the location buffer. These can be one of four defined in LocationBufferCommands
 */
private data class LocationBufferControl(val command: LocationBufferCommands, val location: Location?, val predicate: ((Location) -> Unit) = { _ -> Unit })

internal object MapboxNavigationTelemetry : MapboxNavigationTelemetryInterface {
    private lateinit var context: Context
    private lateinit var mapboxToken: String
    private val TAG = Tag("MAPBOX_TELEMETRY")
    private const val LOCATION_BUFFER_MAX_SIZE = 40
    private const val MAPBOX_NAVIGATION_SDK_IDENTIFIER = "mapbox-navigation-android"
    private const val MOCK_PROVIDER = "com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine"

    private val sessionState = SessionState()
    private val jobControl = ThreadController.getIOScopeAndRootJob()
    private val locationBuffer = mutableListOf<Location>()
    private val channelEventQueue = Channel<Event>(Channel.UNLIMITED)
    private val channelLocationBuffer = Channel<LocationBufferControl>(LOCATION_BUFFER_MAX_SIZE)
    private val channelOnRouteProgress = Channel<RouteProgressWithTimeStamp>(Channel.CONFLATED) // we want just the last notification
    private val channelTelemetryEvent = Channel<MetricEvent>(Channel.CONFLATED) // used in testing to sample the events sent to the server
    private lateinit var metricsReporter: MetricsReporter

    private lateinit var cleanupJob: Job
    private lateinit var locationEngine: LocationEngine
    private lateinit var mapboxTelemetry: MapboxTelemetry

    private lateinit var gpsEventFactory: InitialGpsEventFactory
    private val CURRENT_SESSION_CONTROL: AtomicReference<CurrentSessionState> = AtomicReference(CurrentSessionState.SESSION_END)

    private enum class CurrentSessionState {
        SESSION_START,
        SESSION_END
    }

    private data class RouteProgressWithTimeStamp(val date: Long, val routeProgress: RouteProgress)

    // Call back that receives
    private val routeProgressListener = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            channelOnRouteProgress.offer(RouteProgressWithTimeStamp(Time.SystemImpl.millis(), routeProgress))
        }
    }

    // Return path of the location callback. This will offer data on a channel that serializes location requests
    private val locationCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            ifNonNull(result?.lastLocation) { lastLocation ->
                channelLocationBuffer.offer(LocationBufferControl(LocationBufferCommands.BUFFER_ADD, lastLocation))
            }
        }

        override fun onFailure(exception: Exception) {
            MapboxLogger.i(TAG, Message("location services exception: $exception"))
        }
    }

    /**
     * The lambda that is called if the SDK client did not initialize telemetry. If telemetry is not initialized
     * than all calls to post a telemetry event will fail with this exception
     */
    private val postEventBeforeInit: (TelemetryEvent) -> Unit = { _: TelemetryEvent ->
        throw NavigationException("Telemetry must be initialized before calling this method. Call MapboxNavigationTelemetry.initialize()")
    }

    /**
     * The lambda that is called once telemetry is initialized. This call will genearate a telemetry event. The call is
     * equivalent to postEvent()
     */
    private val postEventAfterInit: (TelemetryEvent) -> Unit = { /*TODO:OZ call from TelemetryMetrics*/ }

    /**
     * The delegate lambda that distaches either a pre or after initialization logic
     */
    private var postEventDelegate = postEventBeforeInit

    /**
     * One-time initializer. Called in response to initialize() and then replaced with a no-op lambda to prevent multiple initialize() calls
     */
    private val primaryInitializer: (Context, String, MapboxNavigation, LocationEngine, MapboxTelemetry, LocationEngineRequest, MetricsReporter) -> Boolean = { context, token, mapboxNavigation, locationEngine, telemetry, locationEngineRequest, metricsReporter ->
        this.context = context
        mapboxToken = token
        this.locationEngine = locationEngine
        validateAccessToken(mapboxToken)
        this.metricsReporter = metricsReporter
        initializer = postInitialize // prevent primaryInitializer() from being called more than once.
        gpsEventFactory = InitialGpsEventFactory(metricsReporter)
        postEventDelegate = postEventAfterInit // now that the object has been initialized we can post events
        registerForNotification(mapboxNavigation)
        mapboxTelemetry = telemetry
        mapboxTelemetry.enable()
        monitorChannels()
        channelOnRouteProgress.offer(RouteProgressWithTimeStamp(Time.SystemImpl.millis(), RouteProgress.Builder().build())) // initially offer an empty route progress so that non-driving telemetry events (like user feedback) can be processed
        /**
         * Register a callback to receive location events. At most [LOCATION_BUFFER_MAX_SIZE] are stored
         */
        locationEngine.requestLocationUpdates(locationEngineRequest, locationCallback, null)
        postTurnstileEvent()
        true
    }
    private var initializer = primaryInitializer
    private var postInitialize: (Context, String, MapboxNavigation, LocationEngine, MapboxTelemetry, LocationEngineRequest, MetricsReporter) -> Boolean = { _, _, _, _, _, _, _ -> false }
    fun initialize(
        context: Context,
        mapboxToken: String,
        mapboxNavigation: MapboxNavigation,
        locationEngine: LocationEngine,
        telemetry: MapboxTelemetry,
        locationEngineRequest: LocationEngineRequest,
        metricsReporter: MetricsReporter
    ) = initializer(context, mapboxToken, mapboxNavigation, locationEngine, telemetry, locationEngineRequest, metricsReporter)

    /**
     * This method starts a unique telemetry session as defined in [SessionState]. startSession/endSession calls are expected to be
     * issued in pairs. To enforce this behavior calling startSession() will terminate the previous session.
     */
    fun startSession(
        directionsRoute: DirectionsRoute,
        locationEngineName: LocationEngine
    ) {
        // Expected session == SESSION_END
        CURRENT_SESSION_CONTROL.compareAndSet(CurrentSessionState.SESSION_END, CurrentSessionState.SESSION_START).let { previousSessionState ->
            when (previousSessionState) {
                true -> {
                    sessionStartHelper(directionsRoute, locationEngineName)
                }
                false -> {
                    endSession()
                    sessionStartHelper(directionsRoute, locationEngineName)
                    MapboxLogger.e(Message("sessionEnd() not called. Calling it by default"))
                }
            }
        }
    }

    /**
     * This method terminates the current session. The call is idempotent.
     */
    fun endSession() {
        CURRENT_SESSION_CONTROL.compareAndSet(CurrentSessionState.SESSION_START, CurrentSessionState.SESSION_END).let { previousSessionState ->
            when (previousSessionState) {
                true -> {
                    sessionEndHelper()
                }
                false -> {
                    // Do nothing. A session cannot be ended twice and calling it multiple times has not detrimental effects
                }
            }
        }
    }

    @TestOnly
    fun pauseTelemetry(flag: Boolean) {
        initializer = when (flag) {
            true -> {
                primaryInitializer
            }
            false -> {
                postInitialize
            }
        }
    }

    @TestOnly
    suspend fun dumpTelemetryJsonPayloadAsync(scope: CoroutineScope): Deferred<String> {
        val result = CompletableDeferred<String>()
        scope.monitorChannelWithException(channelTelemetryEvent, predicate = { event ->
            result.complete(Gson().toJson(event))
        })

        return result
    }

    /**
     * This method is used to post all types of telemetry events to the back-end server.
     * The [event] parameter represents one of several Telemetry events available
     */
    override fun postTelemetryEvent(event: TelemetryEvent) {
        postEventDelegate(event)
    }

    private fun postTurnstileEvent() {
        // AppUserTurnstile is implemented in mapbox-telemetry-sdk
        val appUserTurnstileEvent = AppUserTurnstile(MAPBOX_NAVIGATION_SDK_IDENTIFIER, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME) // TODO:OZ obtain the SDK identifier from MapboxNavigation
        val event = NavigationAppUserTurnstileEvent(appUserTurnstileEvent)
        metricsReporter.addEvent(event)
    }

    /**
     * This method starts a session. The start of a session does not result in a telemetry event being sent to the servers.
     * It is only the initialization of the [SessionState] object with two UUIDs
     */
    private fun sessionStartHelper(
        directionsRoute: DirectionsRoute,
        locationEngine: LocationEngine
    ) {
        jobControl.scope.launch {
            // Initialize identifiers unique to this session
            sessionState.apply {
                sessionIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
                tripIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
            }
            val session = updateSessionState(directionsRoute, locationEngine)
        }
    }

    private suspend fun updateSessionState(directionsRoute: DirectionsRoute, locationEngine: LocationEngine): SessionState {
        sessionState.apply {
            originalDirectionRoute = directionsRoute
            originalRequestIdentifier = directionsRoute.routeOptions()?.requestUuid()
            requestIdentifier = directionsRoute.routeOptions()?.requestUuid()
            currentDirectionRoute = directionsRoute
            eventRouteDistanceCompleted = 0.0
            rerouteCount = 0
            locationEngineName = locationEngine.javaClass.name
            mockLocation = locationEngineName == MOCK_PROVIDER
        }
        try {
            channelOnRouteProgress.receive().also { routeProgressDescriptor ->
                routeProgressDescriptor.routeProgress.currentState()?.let { progressState ->
                    when (progressState) {
                        RouteProgressState.ROUTE_ARRIVED -> {
                            sessionState.arrivalTimestamp = Date(routeProgressDescriptor.date)
                        }
                        else -> {
                            // Do nothing.
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.ifChannelException {
                // Should not be here. This channel is never closed
            }
        }
        return sessionState
    }

    private fun sessionEndHelper() {
        jobControl.scope.launch {
            for (event in channelEventQueue) {
                mapboxTelemetry.push(event)
            }
        }
    }

    private fun sendEvent(telemetryEvent: MetricEvent) {
        channelTelemetryEvent.offer(telemetryEvent)
    }

    private fun registerForNotification(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteProgressObserver(routeProgressListener)
    }

    /**
     * Monitors all channels used in this class. This is done so that access to the containers these channels act on is serialized.
     * By serializing all calls through this choke point, we can guaranty that this class is thread safe.
     */
    private fun monitorChannels() {
        cleanupJob = jobControl.scope.monitorChannelWithException(channelLocationBuffer, { locationAction ->
            when (locationAction.command) {
                LocationBufferCommands.BUFFER_ADD -> {
                    ifNonNull(locationAction.location) { location ->
                        locationBuffer.add(location)
                        if (locationBuffer.size > LOCATION_BUFFER_MAX_SIZE) {
                            locationBuffer.removeAt(0)
                        }
                    }
                }
                LocationBufferCommands.BUFFER_REMOVE -> {
                    if (locationBuffer.size > 0) {
                        locationBuffer.removeAt(0)
                    }
                }
                LocationBufferCommands.BUFFER_CLEAR -> {
                    locationBuffer.clear()
                }
                LocationBufferCommands.BUFFER_READ -> {
                    locationBuffer.forEach { location ->
                        locationAction.predicate(location)
                    }
                }
            }
        })

        /**
         * This monitors the life time of the job that handles the channels. If that job is terminated, we cleanup.
         */
        jobControl.scope.launch {
            select<Unit> {
                cleanupJob.onJoin {
                    locationEngine.removeLocationUpdates(locationCallback)
                }
            }
        }
    }

    private suspend fun getDatePartitionedLocations(predicate: (Location) -> Boolean): Array<Location> {
        val retVal = mutableListOf<Location>()
        channelLocationBuffer.send(LocationBufferControl(LocationBufferCommands.BUFFER_READ, null) { location ->
            if (predicate(location)) {
                retVal.add(location)
            }
        })
        return retVal.toTypedArray()
    }

    /**
     * This method delays sending the event to the server for the duration specified by [delayPeriod]. Once
     * the [delayPeriod] expires, the lambda is executed. This allows the location buffer to be populated
     * with enough data
     */
    private suspend fun delayPostEvent(delayPeriod: Long, predicate: suspend () -> MetricEvent): MetricEvent {
        delay(delayPeriod)
        return predicate()
    }

    private fun validateAccessToken(accessToken: String?) {
        if (accessToken.isNullOrEmpty() ||
                (!accessToken.toLowerCase(Locale.US).startsWith("pk.") &&
                        !accessToken.toLowerCase(Locale.US).startsWith("sk."))
        ) {
            throw NavigationException("A valid access token must be passed in when first initializing MapboxNavigation")
        }
    }
}
