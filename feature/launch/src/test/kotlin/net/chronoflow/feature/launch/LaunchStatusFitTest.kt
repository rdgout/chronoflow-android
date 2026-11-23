package net.chronoflow.feature.launch

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.text.TextPaint
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchLayout
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.MoonTarget
import net.chronoflow.feature.launch.model.StatusLineLayout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchStatusFitTest(
    private val case: LayoutCase,
) {
    @get:Rule val profile = ProfileRule(case)

    @Test fun statusTextAndDotsFitTheSafeBoundsAtBothCappedScales() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val p = case.profile
        val i = p.insets
        val layout =
            LaunchLayout(
                LaunchSize(p.widthDp.toDouble(), p.heightDp.toDouble()),
                MoonTarget.Insets(i.top.toDouble(), i.left.toDouble(), i.bottom.toDouble(), i.right.toDouble()),
            )
        for (language in listOf("en", "nl")) {
            for (scale in listOf(1f, 1.3f)) {
                val configuration =
                    Configuration(context.resources.configuration).apply {
                        setLocales(LocaleList.forLanguageTags(language))
                    }
                val text =
                    context
                        .createConfigurationContext(
                            configuration,
                        ).getString(net.chronoflow.core.ui.R.string.launch_getting_your_plan_ready)
                val paint =
                    TextPaint().apply {
                        typeface =
                            context.resources.getFont(net.chronoflow.core.designsystem.R.font.figtree_400)
                        ;textSize = 14 * scale
                    }
                val metrics = paint.fontMetrics
                val status =
                    StatusLineLayout(
                        p.widthDp.toDouble(),
                        paint.measureText(text).toDouble(),
                        -metrics.ascent.toDouble(),
                        metrics.descent.toDouble(),
                        20.0 * scale,
                        p.density.toDouble(),
                    )
                assertTrue(
                    "${p.name}: $language scale=$scale ink=${status.ink}",
                    layout.safeBounds.contains(status.ink.offset(0.0, layout.status.y)),
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters() = LayoutMatrix.parameters(LayoutMatrix.wide)
    }
}
