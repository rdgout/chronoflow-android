package net.chronoflow.feature.settings.components

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.SettingsBoardFixture
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsComponentsTest : SettingsScreenTest(DeviceProfiles.largeFont) {
    @Test fun choiceExposesRadioRoleAndSelection() {
        var clicked = false
        show {
            SettingsPage(R.string.settings_language, {}, "page") {
                SettingsChoiceCard(
                    stringResource(R.string.settings_english),
                    stringResource(R.string.settings_always_english),
                    true,
                    "choice",
                    { clicked = true },
                )
            }
        }
        compose
            .onNodeWithTag(
                "choice",
            ).assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
            ).performClick()
        assertTrue(clicked)
    }

    @Test fun badgeIsBelowTheSubtitle() {
        show {
            SettingsPage(R.string.settings_settings, null, "page") {
                CfCard(rows = true) {
                    SettingsDestinationRow(
                        stringResource(R.string.settings_body_clock),
                        LucideGlyphs.Moon,
                        "row",
                        {},
                        subtitle = stringResource(R.string.settings_the_answers_every_plan_is_built_from),
                        badge = pluralStringResource(R.plurals.settings_n_to_answer, 3, 3),
                    )
                }
            }
        }
        val subtitle = compose.onNodeWithTag("row.subtitle", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val badge = compose.onNodeWithTag("row.badge", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(badge.top >= subtitle.bottom + 5 * profile.density)
        assertEquals(subtitle.left, badge.left, 1f)
        compose.onNodeWithTag("row.chevron", useUnmergedTree = true).assertExists()
    }

    @Test fun offlineDisabledRowHasNoChevronAndHintGapIsTwelveDp() {
        show { SettingsBoardFixture("SyncStates") }
        compose.onNodeWithTag("settings.disabled.chevron", useUnmergedTree = true).assertDoesNotExist()
        val card =
            compose
                .onNodeWithTag(
                    "settings.disabled.card",
                    useUnmergedTree = true,
                ).fetchSemanticsNode()
                .boundsInRoot
        val hint =
            compose
                .onNodeWithTag(
                    "settings.needsConnection",
                    useUnmergedTree = true,
                ).fetchSemanticsNode()
                .boundsInRoot
        assertEquals(PlanUITokens.space3 * profile.density, hint.top - card.bottom, 1f)
    }

    @Test fun componentsFitAtTheRootFontCap() {
        show {
            SettingsPage(R.string.settings_account, {}, "page") {
                CfCard(rows = true) {
                    SettingsDestinationRow(
                        stringResource(R.string.settings_profile),
                        LucideGlyphs.User,
                        "row",
                        {},
                        subtitle = stringResource(R.string.settings_profile_language),
                    )
                }
                SettingsChoiceCard(
                    stringResource(
                        R.string.settings_english,
                    ),
                    stringResource(R.string.settings_always_english),
                    true,
                    "choice",
                    {
                    },
                )
                SettingsReadOnlyField(stringResource(R.string.settings_email), "rick@example.com", "email")
                SettingsOfflineHint(stringResource(R.string.settings_saved_on_this_phone_syncs_when_y_4fb8b8))
                SettingsNeedsConnection()
            }
        }
        Guards.assertAll(compose, profile, headingTags = emptySet())
    }
}
