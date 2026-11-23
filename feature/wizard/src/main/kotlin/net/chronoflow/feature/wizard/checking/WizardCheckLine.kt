package net.chronoflow.feature.wizard.checking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfSpinner
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardCheckLine(
    line: CheckLine,
    state: CheckLineState,
    index: Int,
) {
    val tag = "wizard.checking.line.$index"
    val label = WizardCopy(line.key).resolve()
    val description =
        if (state == CheckLineState.Open) {
            label
        } else {
            WizardCopy(
                if (state == CheckLineState.Done) "{{line}}, done" else "{{line}}, in progress",
                mapOf("line" to label),
            ).resolve()
        }
    val c = CfTheme.colors
    Row(
        Modifier.testTag(tag).semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            contentDescription = description
        },
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(PlanUITokens.space6.dp)
                .background(
                    if (state == CheckLineState.Done) c.dawn else c.muted,
                    CircleShape,
                ).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                CheckLineState.Done -> {
                    Icon(
                        LucideGlyphs.Check.imageVector(3f),
                        null,
                        Modifier.size(PlanUITokens.wizardSpinnerSize.dp),
                        tint = c.dawnForeground,
                    )
                }

                CheckLineState.Busy -> {
                    if (CfTheme.reduceMotion) {
                        Box(
                            Modifier
                                .testTag(
                                    "$tag.dot",
                                ).size(PlanUITokens.space2.dp)
                                .background(c.mutedForeground, CircleShape),
                        )
                    } else {
                        CfSpinner(
                            Modifier.testTag("$tag.spinner"),
                            size = PlanUITokens.wizardSpinnerSize.dp,
                            color = c.mutedForeground,
                        )
                    }
                }

                CheckLineState.Open -> {}
            }
        }
        Text(
            label,
            Modifier.clearAndSetSemantics { },
            color = if (state == CheckLineState.Done) c.foreground else c.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.wizardSubtitle),
        )
    }
}
