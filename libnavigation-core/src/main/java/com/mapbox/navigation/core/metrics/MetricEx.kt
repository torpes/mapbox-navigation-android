package com.mapbox.navigation.core.metrics

import com.mapbox.android.telemetry.Event

internal fun MetricEvent.toTelemetryEvent(): Event? =
    when (metricName) {
        DirectionsMetrics.ROUTE_RETRIEVAL,
        NavigationMetrics.ARRIVE,
        NavigationMetrics.CANCEL_SESSION,
        NavigationMetrics.DEPART,
        NavigationMetrics.REROUTE,
        NavigationMetrics.FEEDBACK,
        NavigationMetrics.INITIAL_GPS -> this as Event
        NavigationMetrics.APP_USER_TURNSTILE -> (this as NavigationAppUserTurnstileEvent).event
        else -> null
    }
