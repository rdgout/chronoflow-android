package net.chronoflow.feature.launch

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.launch.LaunchEnding
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.generated.LaunchWordmark
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.LaunchFrameSampler
import net.chronoflow.feature.launch.model.LaunchHandOffTimeline
import net.chronoflow.feature.launch.model.LaunchLayout
import net.chronoflow.feature.launch.model.LaunchRect
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchState
import net.chronoflow.feature.launch.model.LaunchTimeline
import net.chronoflow.feature.launch.model.MoonTarget
import net.chronoflow.feature.launch.model.StatusLineLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchStatusGuardTest(
    private val profile: DeviceProfile,
    private val language: AppLanguage,
) {
    @get:Rule val configuration = ProfileRule(profile, language)

    @Test fun drawnStatusFitsTheSafeWidthAndClearsTheWordmark() =
        runTest(timeout = 60.seconds) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val fontScale = ConfigurationRules.capFontScale(profile.fontScale)
            val raster =
                prepareStatusRaster(
                    context,
                    context.getString(net.chronoflow.core.ui.R.string.launch_getting_your_plan_ready),
                    profile.density,
                    fontScale,
                    LaunchAppearance(false).colors.skyTitle.toArgb(),
                    Dispatchers.IO,
                )
            val size = LaunchSize(profile.widthDp.toDouble(), profile.heightDp.toDouble())
            val insets = profile.insets
            val layout =
                LaunchLayout(
                    size,
                    MoonTarget.Insets(
                        insets.top.toDouble(),
                        insets.left.toDouble(),
                        insets.bottom.toDouble(),
                        insets.right.toDouble(),
                    ),
                )
            val status =
                StatusLineLayout(
                    size.width,
                    raster.textWidthDp,
                    raster.ascentDp,
                    raster.descentDp,
                    raster.lineHeightDp,
                    profile.density.toDouble(),
                )
            val timeline =
                LaunchHandOffTimeline.build(
                    LaunchTimeline.opening(size, false),
                    LaunchState(
                        ending = LaunchEnding.App,
                        statusStart = 2.seconds,
                        exitStart = 4600.milliseconds,
                        didCommitExit = true,
                    ),
                )
            val frame = LaunchFrame()
            LaunchFrameSampler(timeline).sampleInto(2.6, frame)
            assertEquals(1.0, frame.statusOpacity, 0.0)
            assertEquals(1.0, frame.containerScale, 0.0)
            val ink = status.ink.offset(0.0, layout.status.minY)
            assertTrue(
                "${profile.name}-${language.tag}: $ink leaves ${layout.safeBounds}",
                ink.minX >= layout.safeBounds.minX && ink.maxX <= layout.safeBounds.maxX,
            )
            val wordmark =
                LaunchRect(
                    layout.wordmarkCentre.x - LaunchWordmark.WIDTH_DP / 2,
                    layout.wordmarkCentre.y - LaunchWordmark.HEIGHT_DP / 2 + frame.wordmarkTranslationY,
                    LaunchWordmark.WIDTH_DP.toDouble(),
                    LaunchWordmark.HEIGHT_DP.toDouble(),
                )
            assertTrue(
                "$ink overlaps $wordmark",
                ink.minY >= wordmark.maxY || ink.maxY <= wordmark.minY || ink.maxX <= wordmark.minX ||
                    ink.minX >= wordmark.maxX,
            )
        }

    companion object {
        @JvmStatic
        @Parameters(name = "{0},{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                DeviceProfiles.small,
                DeviceProfiles.pixel8,
                DeviceProfiles.foldInner,
                DeviceProfiles.landscapeSideNav,
                DeviceProfiles.largeFont,
            ).flatMap { profile ->
                listOf(AppLanguage.En, AppLanguage.Nl).map { language -> arrayOf<Any>(profile, language) }
            }
    }
}
