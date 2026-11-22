@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.testing.compose.warmNativeTextRuntime
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfBannerTest : PrimitiveTestFixture() {
    companion object {
        @JvmStatic @BeforeClass
        fun warmNativeFonts() {
            warmNativeTextRuntime(CfBannerTest::class.java, 36)
        }
    }

    @Test fun politeAnnouncementCanBeSuppressedByTheCaller() {
        host {
            Column {
                CfBanner(stringResource(R.string.settings_saved), null, testTag = "polite")
                CfBanner(stringResource(R.string.settings_saved), null, announce = false, testTag = "silent")
            }
        }
        compose
            .onNodeWithTag(
                "polite",
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertFalse(
            compose
                .onNodeWithTag("silent")
                .fetchSemanticsNode()
                .config
                .contains(SemanticsProperties.LiveRegion),
        )
    }

    @Test fun absentAndEmptyMessagesHaveTheSameTitleOnlyStructure() {
        host {
            Column {
                CfBanner(stringResource(R.string.settings_saved), null, testTag = "absent")
                CfBanner(stringResource(R.string.settings_saved), "", testTag = "empty")
            }
        }
        val absent = compose.onNodeWithTag("absent").fetchSemanticsNode()
        val empty = compose.onNodeWithTag("empty").fetchSemanticsNode()
        assertTrue(absent.config.isMergingSemanticsOfDescendants)
        assertTrue(absent.children.isEmpty())
        assertEquals(absent.boundsInRoot.height, empty.boundsInRoot.height, 0f)
    }

    @Test fun actionRetainsItsOwnTagAndFiresOnce() {
        var calls = 0
        host {
            CfBanner(
                stringResource(R.string.settings_saved),
                null,
                actionLabel = stringResource(R.string.auth_try_again),
                onAction = {
                    calls++
                },
                testTag = "banner",
                actionTestTag = "banner.action",
            )
        }
        compose.onNodeWithTag("banner").assertExists()
        compose.onNodeWithTag("banner.action").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
