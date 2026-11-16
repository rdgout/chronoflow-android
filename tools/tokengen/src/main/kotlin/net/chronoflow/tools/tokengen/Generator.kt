package net.chronoflow.tools.tokengen

import java.nio.file.Path
import java.util.TreeMap

private const val DS = "core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/generated"
private const val PLAN = "core/plan/src/main/kotlin/net/chronoflow/core/plan/generated"

const val COLORS_PATH = "$DS/CfColors.generated.kt"
const val SKY_PATH = "$DS/CfSky.generated.kt"
const val TYPE_PATH = "$DS/CfTypeScale.generated.kt"
const val DIMENS_PATH = "$DS/CfDimens.generated.kt"
const val PALETTE_PATH = "$PLAN/TimelinePalette.generated.kt"
const val PLAN_METRICS_PATH = "$PLAN/PlanMetrics.generated.kt"
const val LAUNCH_LIGHT_PATH = "app/src/main/res/values/colors_launch.xml"
const val LAUNCH_DARK_PATH = "app/src/main/res/values-night/colors_launch.xml"
const val REPORT_PATH = "tools/tokengen/TokenGenReport.md"

/** Every generated output of the token generator, keyed by repo-relative path in sorted order. */
fun tokenOutputs(root: Path): Map<String, ByteArray> {
    val source = DesignSource(root)
    val rows = colorRows(source)
    val press = pressTokens(source.theme)
    val paint = launchPaint(root.design("SplashScreen.dc.html"))
    val planUI = planUITokens(root)
    val auth = authTokens(root)
    val settings = settingsTokens(root, planUI)
    val terms = termsTokens(root, planUI)
    val welcome = welcomeTokens(root)
    val plan = planTokens(root)
    val fonts = fontMetrics(root)
    val skyTop = rows.first { it.name == "sky-top" }
    val outputs = TreeMap<String, ByteArray>()
    outputs[COLORS_PATH] = renderCfColors(rows).toByteArray()
    outputs[SKY_PATH] = renderSky(source, press, paint).toByteArray()
    outputs[TYPE_PATH] = renderTypeScale(plan, fonts, planUI, auth, settings, terms).toByteArray()
    outputs[DIMENS_PATH] = renderDimens(planUI, auth, settings, terms, welcome).toByteArray()
    outputs[PALETTE_PATH] = renderTimelinePalette(rows).toByteArray()
    outputs[PLAN_METRICS_PATH] = renderPlanMetrics(plan).toByteArray()
    outputs[LAUNCH_LIGHT_PATH] = renderLaunchColors(skyTop.light).toByteArray()
    outputs[LAUNCH_DARK_PATH] = renderLaunchColors(skyTop.dark).toByteArray()
    outputs[REPORT_PATH] = renderReport(rows, source).toByteArray()
    return outputs
}
