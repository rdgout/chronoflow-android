package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthFormModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AuthAnnouncerTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun persistentLiveRegionRepeatsOnlyForANewFailure() {
        val form = AuthFormModel()
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) {
                    val state by form.state.collectAsState()
                    val resources = LocalContext.current.resources
                    Column {
                        AuthAnnouncer(
                            state.announcementRevision,
                            { form.takeAnnouncement { it.resolve(resources) } },
                            Modifier.size(48.dp),
                        )
                        state.banner?.let { AuthBanner(it) }
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("auth.announcement").fetchSemanticsNode()
        assertEquals(LiveRegionMode.Polite, before.config[SemanticsProperties.LiveRegion])
        assertFalse(before.config.contains(SemanticsProperties.ContentDescription))
        assertEquals(
            48f,
            (compose.bounds("auth.announcement").right - compose.bounds("auth.announcement").left).value,
            0.5f,
        )
        assertEquals(
            48f,
            (compose.bounds("auth.announcement").bottom - compose.bounds("auth.announcement").top).value,
            0.5f,
        )
        compose.runOnIdle { form.receive(DataFailure.Server(null)) }
        val first = description()
        assertTrue(first.replace("\u200B", "").isNotBlank())
        compose.runOnIdle { form.receive(DataFailure.Server(null)) }
        val second = description()
        assertNotEquals(first, second)
        assertEquals(first.replace("\u200B", ""), second.replace("\u200B", ""))
        assertFalse(
            compose
                .onNodeWithTag("auth.banner")
                .fetchSemanticsNode()
                .config
                .contains(SemanticsProperties.LiveRegion),
        )
        compose.runOnIdle { form.edit("email") }
        assertEquals(second, description())
    }

    private fun description(): String =
        compose
            .onNodeWithTag(
                "auth.announcement",
            ).fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .single()
}
