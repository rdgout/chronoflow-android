package net.chronoflow.feature.onboarding.walkthrough

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.system.CfLinearProgress
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.plan.drawing.TimelineCategoryStyle
import net.chronoflow.core.plan.drawing.color
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.plan.CategorySwatch
import net.chronoflow.feature.onboarding.OnboardingMetrics

@Composable
internal fun WalkthroughPreview(page: WalkthroughModel.Page) {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(WalkthroughTags.PREVIEW)
            .clearAndSetSemantics {}
            .softShadow(
                shape,
                colors.isDark,
            ).background(colors.card, shape)
            .border(AuthTokens.borderWidth.dp, colors.border, shape)
            .padding(PlanUITokens.space5.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.walkthroughCopyGap.dp),
    ) {
        when (page) {
            WalkthroughModel.Page.Timeline -> {
                PreviewBlock(BlockType.Sleep, R.string.walkthrough_sleep, R.string.walkthrough_23_0007_00)
                PreviewBlock(
                    BlockType.SeekLight,
                    R.string.walkthrough_seek_bright_light,
                    R.string.walkthrough_08_0010_00,
                )
                PreviewBlock(
                    BlockType.CaffeineServing,
                    R.string.walkthrough_1_cup_of_coffee,
                    R.string.walkthrough_09_30,
                )
                PreviewBlock(BlockType.Flight, R.string.walkthrough_kl_861, R.string.walkthrough_14_4009_15)
            }

            WalkthroughModel.Page.Categories -> {
                CategoryRow(BlockType.Sleep, R.string.walkthrough_sleep)
                CategoryRow(BlockType.SeekLight, R.string.walkthrough_seek_bright_light)
                CategoryRow(BlockType.AvoidLight, R.string.walkthrough_avoid_light)
                CategoryRow(BlockType.CaffeineServing, R.string.walkthrough_caffeine)
                CategoryRow(BlockType.Flight, R.string.walkthrough_flight)
            }

            WalkthroughModel.Page.Progress -> {
                FlowRow(
                    Modifier.padding(top = PlanUITokens.space1.dp),
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                    verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                ) {
                    CfLinearProgress({
                        3f / 7f
                    }, Modifier.width(PlanUITokens.progressTrackWidth.dp).height(OnboardingMetrics.PROGRESS_HEIGHT.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            LucideGlyphs.Check.imageVector(),
                            null,
                            Modifier.size(PlanUITokens.space3.dp),
                            tint = colors.mutedForeground,
                        )
                        Text(
                            stringResource(R.string.walkthrough_done_total_done, "3", "7"),
                            color = colors.mutedForeground,
                            style = CfTheme.type.style(CfTypeScale.screenMeta).copy(fontFeatureSettings = "tnum"),
                        )
                    }
                }
                ProgressRow(
                    BlockType.SeekLight,
                    R.string.walkthrough_seek_bright_light_outdoors,
                    R.string.walkthrough_11_00,
                    R.string.walkthrough_in_15_min,
                )
                ProgressRow(
                    BlockType.CaffeineServing,
                    R.string.walkthrough_1_cup_of_filter_coffee,
                    R.string.walkthrough_10_45,
                    R.string.walkthrough_done,
                )
            }
        }
    }
}

@Composable
private fun PreviewBlock(
    type: BlockType,
    title: Int,
    time: Int,
) {
    val colors = CfTheme.colors
    val style = remember(type) { TimelineCategoryStyle(type) }
    val palette = if (colors.isDark) TimelinePalette.Dark else TimelinePalette.Light
    val fill = Color(palette.color(style.fill))
    val ink = Color(palette.color(style.foreground))
    val caffeine = type == BlockType.CaffeineServing
    val shape = RoundedCornerShape((if (caffeine) PlanUITokens.radiusFull else PlanUITokens.radiusLg).dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = PlanUITokens.walkthroughBlockHeight.dp,
            ).clip(shape)
            .background(fill)
            .drawWithCache {
                val path = Path()
                if (caffeine) {
                    val spacing = PlanUITokens.patternDotSpacing.dp.toPx()
                    val diameter = PlanUITokens.patternDotDiameter.dp.toPx()
                    var x = 0f
                    while (x <= size.width) {
                        var y = 0f
                        while (y <= size.height) {
                            path.addOval(Rect(Offset(x, y), Size(diameter, diameter)))
                            y += spacing
                        }
                        x += spacing
                    }
                }
                val dotInk = ink.copy(alpha = PlanUITokens.patternDotOpacity)
                val coreInk = colors.foreground.copy(alpha = PlanUITokens.priorityCoreOpacity)
                val coreSize = Size(PlanUITokens.space1.dp.toPx(), size.height)
                onDrawBehind { if (caffeine) drawPath(path, dotInk) else drawRect(coreInk, size = coreSize) }
            }.padding((PlanUITokens.space1 + PlanUITokens.space1 / 2).dp),
        contentAlignment = Alignment.Center,
    ) {
        val compact = maxWidth.value < PlanUITokens.walkthroughCompactWidth
        Column(verticalArrangement = Arrangement.spacedBy((PlanUITokens.space1 / 2).dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    if (compact) {
                        Arrangement.Center
                    } else {
                        Arrangement.spacedBy(
                            PlanUITokens.space1.dp,
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val glyph =
                    when (type) {
                        BlockType.Sleep -> LucideGlyphs.Moon
                        BlockType.SeekLight -> LucideGlyphs.Sun
                        BlockType.CaffeineServing -> LucideGlyphs.Coffee
                        else -> LucideGlyphs.Plane
                    }
                Icon(
                    glyph.imageVector(),
                    null,
                    Modifier.size((PlanUITokens.space3 + PlanUITokens.space1 / 2).dp),
                    tint = ink,
                )
                if (!compact) {
                    Text(
                        if (caffeine) {
                            stringResource(
                                R.string.walkthrough_title_time,
                                stringResource(title),
                                stringResource(time),
                            )
                        } else {
                            stringResource(title)
                        },
                        color = ink,
                        style = CfTheme.type.style(CfTypeScale.screenBlockLabel),
                    )
                }
            }
            if (!compact &&
                !caffeine
            ) {
                Text(stringResource(time), color = ink, style = CfTheme.type.style(CfTypeScale.screenBlockLabel))
            }
        }
    }
}

@Composable
private fun CategoryRow(
    type: BlockType,
    title: Int,
) {
    Row(
        Modifier.heightIn(min = AuthTokens.touch.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategorySwatch(type = type)
        Text(
            stringResource(title),
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.walkthroughBody),
        )
    }
}

@Composable
private fun ProgressRow(
    type: BlockType,
    title: Int,
    time: Int,
    relative: Int,
) {
    Row(
        Modifier.padding(PlanUITokens.space2.dp).heightIn(min = PlanUITokens.space14.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategorySwatch(type = type)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(title),
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.walkthroughBody),
            )
            Text(
                stringResource(
                    R.string.walkthrough_time_zone_relative,
                    stringResource(time),
                    stringResource(R.string.walkthrough_tokyo),
                    stringResource(relative),
                ),
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.screenBody.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}
