package net.chronoflow.feature.launch.welcome

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

// iOS ExactLineText centers natural-height ink over exact line boxes; keep Android's boundary padding out of layout too.
internal fun Modifier.welcomeLineHeight(lineHeight: TextUnit): Modifier =
    layout { measurable, constraints ->
        val text = measurable.measure(constraints)
        val pitch = lineHeight.toPx()
        val lines = ((text[LastBaseline] - text[FirstBaseline]) / pitch).roundToInt() + 1
        val height = (lines * pitch).roundToInt()
        layout(text.width, height) { text.place(0, -((text.height - height) / 2f).roundToInt()) }
    }
