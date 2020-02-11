package com.mapbox.navigation.core

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.thread.JobControl
import com.mapbox.navigation.utils.thread.ThreadController
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

class MapboxNavigationTelemetryTest {
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation>()
    private val mockLocationEngine = mockk<LocationEngine>()
    private val mockLocationEngineRequest = mockk<LocationEngineRequest>()
    private val telemetry = mockk<MapboxTelemetry>()
    private var token = "pk.1234.PABLO'S-FAKE-TOKEN"
    private val mainCoroutineRule = MainCoroutineRule()

    private var expectedJson = "{\"metricName\":\"navigation.feedback\",\"userFeedback\":{\"feedbackType\":\"FEEDBACK_TYPE_ACCIDENT\",\"description\":\"big bad accident\",\"source\":\"FEEDBACK_SOURCE_USER\",\"screenShot\":\"screen shot\"},\"userId\":\"b1962a72-58eb-42f9-b76f-0cbd363950de\",\"audio\":\"unknown\",\"locationsBefore\":[],\"locationsAfter\":[],\"feedbackId\":\"779c8b02-06fd-4073-adb2-dbfc7c66b860\",\"screenshot\":\"screen shot\",\"step\":{\"upcomingType\":\"\",\"upcomingModifier\":\"\",\"upcomingName\":\"\",\"previousType\":\"\",\"previousModifier\":\"\",\"previousName\":\"\",\"distance\":0,\"duration\":0,\"distanceRemaining\":0,\"durationRemaining\":0}}"
    @Before
    fun setUp() {
        every { telemetry.enable() } returns true
        every { mockContext.applicationContext } returns mockContext
        every { mockNavigation.registerRouteProgressObserver(any()) } answers {}
        every { mockLocationEngine.requestLocationUpdates(any(), any<LocationEngineCallback<LocationEngineResult>>(), null) } just Runs
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun mockIOScopeAndRootJob(): CoroutineScope {
        mockkObject(ThreadController)
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + TestCoroutineDispatcher())
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.IODispatcher } returns TestCoroutineDispatcher()
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        return testScope
    }

    @Test
    fun TelemetryInitTest() {
        // assert that the first call to initialize() returns true and the second returns false
        MapboxNavigationTelemetry.pauseTelemetry(true)
        assert(MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, mockLocationEngine, telemetry, mockLocationEngineRequest))
        assert(!MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, mockLocationEngine, telemetry, mockLocationEngineRequest))
    }
}
