package net.chronoflow.feature.trip.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HeadingZoneLayoutTest : PageComponentTest() {
    private fun start(width: Int) {
        host {
            Box(Modifier.width(width.dp)) {
                HeadingZoneLayout(
                    heading = { Spacer(Modifier.size(220.dp, 26.dp).testTag("heading")) },
                    zone = { Spacer(Modifier.size(60.dp, 15.dp).testTag("zone")) },
                )
            }
        }
    }

    @Test fun wideHeadingAndZoneShareTheBaselineRow() {
        start(360)
        val heading = compose.onNodeWithTag("heading").getUnclippedBoundsInRoot()
        val zone = compose.onNodeWithTag("zone").getUnclippedBoundsInRoot()
        assertEquals(heading.bottom, zone.bottom)
        assertTrue(zone.left >= heading.right)
    }

    @Test fun narrowHeadingAndZoneUseAColumn() {
        start(200)
        val heading = compose.onNodeWithTag("heading").getUnclippedBoundsInRoot()
        val zone = compose.onNodeWithTag("zone").getUnclippedBoundsInRoot()
        assertTrue(zone.top >= heading.bottom)
    }
}
