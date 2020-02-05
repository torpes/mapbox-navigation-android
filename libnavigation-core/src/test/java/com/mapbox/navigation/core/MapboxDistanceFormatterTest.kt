package com.mapbox.navigation.core

import android.content.Context
import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.typedef.IMPERIAL
import com.mapbox.navigation.base.typedef.METRIC
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import org.hamcrest.core.IsInstanceOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class MapboxDistanceFormatterTest {

    val ctx = ApplicationProvider.getApplicationContext<Context>()

    @Config(qualifiers = "en")
    @Test
    fun formatDistance_when_distance_greaterThan_10_milesOrKilometers() {
        val formatter = MapboxDistanceFormatter(ctx, "en", IMPERIAL, ROUNDING_INCREMENT_FIFTY)

        val result = formatter.formatDistance(20000.0)

        assertEquals("12 mi", result.toString())
        assertEquals(2, result.getSpans(0, result.count(), Object::class.java).size)
        assertThat(result.getSpans(0, result.count(), Object::class.java)[0], IsInstanceOf(StyleSpan::class.java) )
        assertEquals(Typeface.BOLD, (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style)
        assertThat(result.getSpans(0, result.count(), Object::class.java)[1], IsInstanceOf(RelativeSizeSpan::class.java) )
        assertEquals(0.65f, (result.getSpans(0, result.count(), Object::class.java)[1] as RelativeSizeSpan).sizeChange)
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistance_when_distance_lessThan_401_feetOrMeters() {
        val formatter = MapboxDistanceFormatter(ctx, "en", METRIC, ROUNDING_INCREMENT_FIFTY)

        val result = formatter.formatDistance(10.0)

        assertEquals("50 m", result.toString())
    }

    @Config(qualifiers = "ja")
    @Test
    fun formatDistance_when_distance_lessThan_401_feetOrMeters_Japanese() {
        val formatter = MapboxDistanceFormatter(ctx, "ja", IMPERIAL, ROUNDING_INCREMENT_FIFTY)

        val result = formatter.formatDistance(10.0)

        assertEquals("50 フィート", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistance_when_distance_between_401_feetOrMeters_and_10_milesOrKilometers() {
        val formatter = MapboxDistanceFormatter(ctx, "en", METRIC, ROUNDING_INCREMENT_FIFTY)

        val result = formatter.formatDistance(1000.0)

        assertEquals("1 km", result.toString())
    }

    @Config(qualifiers = "en")
    @Test
    fun formatDistance_spans() {
        val formatter = MapboxDistanceFormatter(ctx, "en", IMPERIAL, ROUNDING_INCREMENT_FIFTY)

        val result = formatter.formatDistance(100.0)

        assertThat(result.getSpans(0, result.count(), Object::class.java)[0], IsInstanceOf(StyleSpan::class.java) )
        assertEquals(Typeface.BOLD, (result.getSpans(0, result.count(), Object::class.java)[0] as StyleSpan).style)
        assertThat(result.getSpans(0, result.count(), Object::class.java)[1], IsInstanceOf(RelativeSizeSpan::class.java) )
        assertEquals(0.65f, (result.getSpans(0, result.count(), Object::class.java)[1] as RelativeSizeSpan).sizeChange)
    }
}