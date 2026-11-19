package net.chronoflow.feature.trip.step

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.TripPage
import net.chronoflow.feature.trip.page.TripPageTestRig
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CustomActionAnnouncesTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var rig: TripPageTestRig

    @After fun close() {
        if (::rig.isInitialized) rig.close()
    }

    @Test fun customActionAnnouncesWhenTheMarkLandsWithoutHaptic() {
        rig = TripPageTestRig("S1", initialTimeline = false, applyChecks = true)
        val haptics = RecordingHaptics()
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme {
                    CompositionLocalProvider(LocalHaptics provides haptics) { TripPage(rig.vm, {}, null) }
                }
            }
        }
        val id =
            rig.model.prepared.days
                .first()
                .blocks
                .first { it.block.actionable && it.block.checkKey != null }
                .id
        val actions =
            compose
                .onNodeWithTag(
                    "trip.block.$id",
                ).fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        compose.runOnIdle { actions.first().action() }
        compose.waitForIdle()
        val value =
            compose
                .onNodeWithTag(
                    "plan.announcer",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertTrue(value.single().isNotEmpty())
        assertTrue(haptics.events.isEmpty())
    }
}
