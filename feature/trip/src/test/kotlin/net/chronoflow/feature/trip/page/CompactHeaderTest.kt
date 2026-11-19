package net.chronoflow.feature.trip.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompactHeaderTest : PageComponentTest() {
    @Test fun aPartlyTransparentHeaderHidesItsChildSemantics() {
        val day = prepared.days[0]
        host { CompactHeader(day, { 0.5f }, false, PaddingValues()) }
        compose.onNodeWithTag("trip.compactHeader").onChildren().assertCountEquals(0)
    }

    @Test fun anOpaqueHeaderExposesOneDayDescription() {
        val day = prepared.days[0]
        host { CompactHeader(day, { 1f }, true, PaddingValues()) }
        compose.onNodeWithTag("trip.compactHeader").onChildren().assertCountEquals(1)
    }

    @Test fun touchesReachTheListUnderneathTheOverlay() {
        val day = prepared.days[0]
        var clicks = 0
        host {
            Box {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { clicks++ }
                        .testTag("underneath"),
                )
                CompactHeader(day, { 1f }, true, PaddingValues())
            }
        }
        compose.onNodeWithTag("trip.compactHeader").performTouchInput { click(center) }
        assertEquals(1, clicks)
    }
}
