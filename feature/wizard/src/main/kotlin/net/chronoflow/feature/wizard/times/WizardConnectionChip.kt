package net.chronoflow.feature.wizard.times

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.wizard.model.WizardConnection
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun wizardDuration(minutes: Int): String =
    WizardCopy(
        "{{hours}} h {{minutes}} min",
        mapOf("hours" to (minutes / 60).toString(), "minutes" to (minutes % 60).toString()),
    ).resolve()

@Composable
internal fun WizardConnectionChip(
    fact: WizardConnection?,
    index: Int,
) {
    val color = CfTheme.colors.border
    val rule =
        Modifier.height(PlanUITokens.wizardStopDotWidth.dp).clearAndSetSemantics { }.drawWithCache {
            val stroke = PlanUITokens.wizardStopDotWidth.dp.toPx()
            val dash =
                PathEffect.dashPathEffect(
                    floatArrayOf(stroke, stroke),
                )
            val start = Offset(0f, size.height / 2)
            val end = Offset(size.width, size.height / 2)
            onDrawBehind { drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Butt, pathEffect = dash) }
        }
    Row(
        Modifier.fillMaxWidth().testTag("wizard.connection.$index"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
    ) {
        Box(rule.weight(1f))
        if (fact != null) {
            val copy =
                when (fact) {
                    is WizardConnection.Stop -> {
                        WizardCopy("Stop in {{airport}}", mapOf("airport" to fact.airport))
                    }

                    is WizardConnection.Connection -> {
                        WizardCopy(
                            "Connection in {{airport}} · {{duration}}",
                            mapOf("airport" to fact.airport, "duration" to wizardDuration(fact.minutes)),
                        )
                    }

                    is WizardConnection.Overnight -> {
                        WizardCopy(
                            if (fact.nights ==
                                1
                            ) {
                                "Stop in {{airport}} · {{count}} complete night"
                            } else {
                                "Stop in {{airport}} · {{count}} complete nights"
                            },
                            mapOf("airport" to fact.airport, "count" to fact.nights.toString()),
                        )
                    }
                }
            Text(
                copy.resolve(),
                Modifier
                    .background(
                        CfTheme.colors.surface2,
                        CircleShape,
                    ).padding(horizontal = PlanUITokens.space3.dp, vertical = WizardMetrics.chipPaddingVertical.dp),
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.wizardHint).copy(fontFeatureSettings = "tnum"),
            )
        }
        Box(rule.weight(1f))
    }
}
