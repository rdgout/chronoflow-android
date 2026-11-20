package net.chronoflow.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.R
import net.chronoflow.core.designsystem.generated.CfFace
import net.chronoflow.core.designsystem.generated.CfTypeMetrics
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.CfTypeSpec
import net.chronoflow.core.designsystem.generated.PlanTypeMetrics
import net.chronoflow.core.designsystem.generated.TypeMetrics
import java.util.IdentityHashMap

val Figtree =
    FontFamily(
        Font(R.font.figtree_400, FontWeight.W400),
        Font(R.font.figtree_500, FontWeight.W500),
        Font(R.font.figtree_600, FontWeight.W600),
        Font(R.font.figtree_700, FontWeight.W700),
    )
val Fraunces20 =
    FontFamily(Font(R.font.fraunces_400_opsz20, FontWeight.W400), Font(R.font.fraunces_500_opsz20, FontWeight.W500))
val Fraunces32 =
    FontFamily(Font(R.font.fraunces_400_opsz32, FontWeight.W400), Font(R.font.fraunces_500_opsz32, FontWeight.W500))
val Fraunces48 =
    FontFamily(Font(R.font.fraunces_400_opsz48, FontWeight.W400), Font(R.font.fraunces_500_opsz48, FontWeight.W500))

fun CfFace.fontFamily(): FontFamily =
    when (this) {
        CfFace.System400 -> FontFamily.Default
        CfFace.Figtree400, CfFace.Figtree500, CfFace.Figtree600, CfFace.Figtree700 -> Figtree
        CfFace.Fraunces400Opsz20, CfFace.Fraunces500Opsz20 -> Fraunces20
        CfFace.Fraunces400Opsz32, CfFace.Fraunces500Opsz32 -> Fraunces32
        CfFace.Fraunces400Opsz48, CfFace.Fraunces500Opsz48 -> Fraunces48
    }

fun CfFace.fontWeight(): FontWeight =
    when (this) {
        CfFace.System400,
        CfFace.Figtree400,
        CfFace.Fraunces400Opsz20,
        CfFace.Fraunces400Opsz32,
        CfFace.Fraunces400Opsz48,
        -> FontWeight.W400

        CfFace.Figtree500,
        CfFace.Fraunces500Opsz20,
        CfFace.Fraunces500Opsz32,
        CfFace.Fraunces500Opsz48,
        -> FontWeight.W500

        CfFace.Figtree600 -> FontWeight.W600

        CfFace.Figtree700 -> FontWeight.W700
    }

fun CfTypeSpec.toTextStyle(): TextStyle =
    TextStyle(
        fontFamily = face.fontFamily(),
        fontWeight = face.fontWeight(),
        fontSynthesis = if (allowFontSynthesis) null else FontSynthesis.None,
        fontSize = size.sp,
        lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
        letterSpacing = letterSpacingEm.em,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

fun TextStyle.tnum(): TextStyle = copy(fontFeatureSettings = "tnum")

private fun CfTypeMetrics.spec(face: CfFace) = CfTypeSpec(face, size, lineHeight, letterSpacingEm)

@Immutable
class CfType {
    private val styles =
        IdentityHashMap<CfTypeSpec, TextStyle>().apply {
            listOf(
                CfTypeScale.screenHero,
                CfTypeScale.screenPageTitle,
                CfTypeScale.screenCardTitle,
                CfTypeScale.screenSectionTitle,
                CfTypeScale.screenDayTitle,
                CfTypeScale.screenBodyLg,
                CfTypeScale.screenBody,
                CfTypeScale.screenLabel,
                CfTypeScale.screenMeta,
                CfTypeScale.screenEyebrow,
                CfTypeScale.screenBlockLabel,
                CfTypeScale.screenPriorityTag,
                CfTypeScale.walkthroughBody,
                CfTypeScale.wizardReadySummary,
                CfTypeScale.wizardFailureTitle,
                CfTypeScale.wizardFailureBody,
                CfTypeScale.wizardSecondary,
                CfTypeScale.wizardArc,
                CfTypeScale.wizardNavTitle,
                CfTypeScale.wizardSearchCode,
                CfTypeScale.wizardSearchName,
                CfTypeScale.wizardLongTitle,
                CfTypeScale.wizardAirport,
                CfTypeScale.wizardAirportPlaceholder,
                CfTypeScale.wizardRow,
                CfTypeScale.wizardButton,
                CfTypeScale.wizardSheetTitle,
                CfTypeScale.wizardSubtitle,
                CfTypeScale.wizardChronotype,
                CfTypeScale.wizardFieldLabel,
                CfTypeScale.wizardHint,
                CfTypeScale.wizardChoice,
                CfTypeScale.wizardRoute,
                CfTypeScale.authBrand,
                CfTypeScale.authHeading,
                CfTypeScale.authBody,
                CfTypeScale.authLabel,
                CfTypeScale.authStrong,
                CfTypeScale.authHint,
                CfTypeScale.authMeta,
                CfTypeScale.settingsHint,
                CfTypeScale.settingsPromptBody,
                CfTypeScale.settingsCodes,
                CfTypeScale.settingsTime,
                CfTypeScale.settingsEyebrow,
                CfTypeScale.settingsRow,
                CfTypeScale.settingsStrong,
                CfTypeScale.settingsPill,
                CfTypeScale.settingsBadge,
                CfTypeScale.termsTitle,
                CfTypeScale.termsBody,
                CfTypeScale.termsStrong,
            ).forEach { put(it, it.toTextStyle()) }
        }

    fun style(spec: CfTypeSpec): TextStyle = styles[spec] ?: spec.toTextStyle()

    val planCardTitle = PlanTypeMetrics.planCardTitle.spec(CfFace.Fraunces400Opsz20).toTextStyle()
    val planDayTitle = PlanTypeMetrics.planDayTitle.spec(CfFace.Fraunces400Opsz20).toTextStyle()
    val planRemoteTitle = PlanTypeMetrics.planRemoteTitle.spec(CfFace.Fraunces400Opsz20).toTextStyle()
    val planTitle = PlanTypeMetrics.planTitle.spec(PlanTypeMetrics.planTitleFace).toTextStyle()
    val planSubtitle = PlanTypeMetrics.planSubtitle.spec(PlanTypeMetrics.planSubtitleFace).toTextStyle()
    val planLane = PlanTypeMetrics.planLane.spec(PlanTypeMetrics.planLaneFace).toTextStyle()
    val planCompactDate = PlanTypeMetrics.planCompactDate.spec(PlanTypeMetrics.planCompactDateFace).toTextStyle()
    val planCompactWeekday =
        PlanTypeMetrics.planCompactWeekday
            .spec(
                PlanTypeMetrics.planCompactWeekdayFace,
            ).toTextStyle()
    val planListTitle = PlanTypeMetrics.planListTitle.spec(PlanTypeMetrics.planListTitleFace).toTextStyle()
    val planBlockLabel = PlanTypeMetrics.planBlockLabel.spec(PlanTypeMetrics.planBlockLabelFace).toTextStyle()
    val planNow = PlanTypeMetrics.planNow.spec(PlanTypeMetrics.planNowFace).toTextStyle()
    val planPriority = PlanTypeMetrics.planPriority.spec(PlanTypeMetrics.planPriorityFace).toTextStyle()
    val planListLine = PlanTypeMetrics.planListLine.spec(PlanTypeMetrics.planListLineFace).toTextStyle()
    val splashWordmark = TypeMetrics.splashWordmark.spec(CfFace.Fraunces400Opsz32).toTextStyle()
    val status = TypeMetrics.body.spec(CfFace.Figtree400).toTextStyle()
    val welcomeWordmark = TypeMetrics.sectionTitle.spec(CfFace.Fraunces400Opsz20).toTextStyle()
    val eyebrow = TypeMetrics.eyebrow.spec(CfFace.Figtree600).toTextStyle()
    val headline = TypeMetrics.hero.spec(CfFace.Fraunces400Opsz48).toTextStyle()
    val copy = TypeMetrics.bodyLg.spec(CfFace.Figtree400).toTextStyle()
    val primaryButton = TypeMetrics.button.spec(CfFace.Figtree600).toTextStyle()
    val secondaryButton = TypeMetrics.button.spec(CfFace.Figtree500).toTextStyle()
    val authLabel = style(CfTypeScale.authLabel)
    val authStrong = style(CfTypeScale.authStrong)
    val authHint = style(CfTypeScale.authHint)
    val authBody = style(CfTypeScale.authBody)
    val settingsRow = style(CfTypeScale.settingsRow)
    val settingsPill = style(CfTypeScale.settingsPill)
    val settingsBadge = style(CfTypeScale.settingsBadge)
    val screenBody = style(CfTypeScale.screenBody)
    val screenLabel = style(CfTypeScale.screenLabel)
}
