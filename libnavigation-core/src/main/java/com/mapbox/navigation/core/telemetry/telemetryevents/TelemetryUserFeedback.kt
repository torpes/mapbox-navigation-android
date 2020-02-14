package com.mapbox.navigation.core.telemetry.telemetryevents

import android.annotation.SuppressLint
import android.location.Location
import android.os.Parcel
import com.mapbox.android.telemetry.Event

/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

// Defaulted values are optional
@SuppressLint("ParcelCreator")
class TelemetryUserFeedback(
    val feedbackType: String,
    val description: String? = null,
    val source: String,
    val userId: String,
    val locationsBefore: Array<Location>,
    val locationsAfter: Array<Location>,
    val feedbackId: String,
    val screenshot: String? = null,
    val step: TelemetryStep? = null,
    var Metadata: TelemetryMetadata
) : Event() {
    val event = "navigation.feedback"
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
    override fun describeContents() = 0
}
