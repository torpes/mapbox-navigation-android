package com.mapbox.navigation.core.telemetry.telemetryevents

import android.os.Build

const val CurrentVersion = 4
/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

// Defaulted values are optional, except eventVersion. That value is currently '4' and should be changed once it changes on the server
data class TelemetryMetadata(
    val created: String, // ISO 8601 + UTC Formatted timestamp (in milliseconds): yyyy-MM-dd'T'HH:mm:ss.SSSZ string  Required
    val startTimestamp: String, // timestamp when user started navigation string  Required
    val operatingSystem: String = "Android - ${Build.VERSION.RELEASE}", // Name and version of operating system e.g. ios-10.3.2 string  Required
    val device: String, // Identifier for the device e.g. iPhone7,1                                                                                                                                                                                                                                                                              string  Required
    val sdkIdentifier: String, // e.g. mapbox-navigation-ui-ios, mapbox-navigation-ios, mapbox-navigation-ui-android, mapbox-navigation-android string  Required
    val sdkVersion: String, // SDK version string e.g. 0.3.0 string  Required
    val eventVersion: Int = CurrentVersion, // Integer to version event payloads, so that it’s easier to handle event format changes later; currently 4 int     Required
    val profile: String = "mapbox/driving-traffic", // E.g. mapbox/driving-traffic, mapbox/cycling string  Required
    val simulation: Boolean = false, // Bool that indicates whether route simulation was enabled in the SDK bool    Required
    val locationEngine: String, // String representing the class name of the location engine used for navigation, e.g. CLLocationManager or NavigationLocationManager on iOS, and LostLocationEngine and GoogleLocationEngine, AndroidLocationEngine on Android string  Required  6
    val sessionIdentifier: String, // Random UUID unique to a navigation session to be able to associate navigation events through a full session string  Required
    val originalRequestIdentifier: String? = null, // UUID that matches the original directions API request. This can be used to find the original directions response in our logs. string  Optional
    val requestIdentifier: String? = null, // UUID that matches a directions API request. This can be used to find the original directions response in our logs. string  Optional
    val lat: Float, // Latitude of current location when event occurred float   Required
    val lng: Float, // Longitude of current location when event occurred float   Required
    val originalGeometry: String? = null, // Encoded 5 decimal precision polyline of the original route geometry. This should be the geometry from the very first route request, not from subsequent reroute requests. string  Optional
    val originalEstimatedDistance: Int = 0, // Total estimated route distance in meters for the original route request. In the case of reroutes and ETA updates, this should always equal the estimated distance from the original request. int     Optional
    val originalEstimatedDuration: Int = 0, // Estimated route duration in seconds from the original route request. In the case of reroutes and ETA updates, this should always equal the estimated duration from the original request. int     Optional
    val originalStepCount: Int = 0, // Count of the total number of maneuvers/steps in the original directions response. int     Optional
    val geometry: String, // Encoded 5 decimal precision polyline of the current route geometry. This may be different from the originalGeometry attribute which contains the original geometry from the original navigation.depart event. string  Required
    val estimatedDistance: Int = 0, // Optional Total estimated route distance in meters for the current route request. In the case of reroutes and ETA updates, this should always equal the estimated distance for the current route. int     Optional
    val estimatedDuration: Int = 0, // Optional Estimated route duration in seconds from the current route request. In the case of reroutes and ETA updates, this should always equal the estimated duration for the current route. int     Optional
    val stepCount: Int = 0, // Optional Count of the total number of maneuvers/steps in the current directions response int     Optional
    val distanceCompleted: Int, // Total distance in meters travelled since the start of the navigation session (across all route updates) int     Required
    val distanceRemaining: Int, // Remaining distance in meters (in the case of navigation.feedback and navigation.reroute this is the distance before the event was triggered) int     Required
    val absoluteDistanceToDestination: Int, // As-the-crow-flies distance to the destination coordinate. Absolute distance to destination may be different than distanceRemaining if the route wraps around the block or if the destination isn’t close to a road. int     Required  6
    val durationRemaining: Int, // Remaining duration in seconds (in the case of navigation.feedback and navigation.reroute this is the duration before the event was triggered) int     Required
    val rerouteCount: Int = 0, // Optional                   Count of the number of reroutes during this navigation session that have occurred before but not counting this event int     Optional
    val volumeLevel: Int = 0, // Int from 0 (muted) to 100 (full volume). Useful for knowing if the user is using the app with the volume down. Should be set to 0 if the mute/vibrate switch is enabled. int     Optional
    val audioType: String? = null, // Audio output: speaker, bluetooth, headphones, unknown string  Optional
    val screenBrightness: Int = 0, // Optional               Int from 0 (screen off) to 100 (full brightness). Useful for knowing if the user is using the app with the screen on int     Optional
    val applicationState: String? = null, // e.g. backgrounded, active, etc. Another alternative to screenBrightness for determining if the user is using the app in the foreground or background string  Optional
    val percentTimeInForeground: Int = 0, // Optional         The current percent (from 0  to 100) of the elapsed navigation session where the current app was displayed in the foreground. Keep track of the total time navigation is in the foreground and compare against the time since the start of the navigation session to compute the current percentTimeInForeground  int     Optional  6
    val percentTimeInPortrait: Int = 0, // Optional           Of the time when the app is in the foreground (see percentTimeInForeground), represents the current percent (from 0 to 100) where the app was in portrait orientation. int     Optional  6
    val batteryPluggedIn: Boolean = false, // Optional              e.g. true if charging or full, false if discharging or unknown bool    Optional
    val batteryLevel: Int = 0, // Optional                    Int from 0 (empty) to 100 (fully charged); -1 if unknownint     Optional
    val connectivity: String? = null // E.g. none, wifi, gprs, edge, 3g, 4g, lte                                                                                                                                                                                                                                                                  string  Optional
)
