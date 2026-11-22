@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@file:Suppress("ktlint:compose:modifier-missing-check")

package net.chronoflow.core.testing.compose.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.component.CfNotice
import net.chronoflow.core.designsystem.component.CfNoticeTone
import net.chronoflow.core.designsystem.component.CfPill
import net.chronoflow.core.designsystem.component.CfPillGroup
import net.chronoflow.core.designsystem.component.CfPillOption
import net.chronoflow.core.designsystem.component.CfRow
import net.chronoflow.core.designsystem.component.CfSecureTextField
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.designsystem.component.CfSpinner
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.generated.WelcomeTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfAlertDialog
import net.chronoflow.core.designsystem.system.CfBackButton
import net.chronoflow.core.designsystem.system.CfDatePickerDialog
import net.chronoflow.core.designsystem.system.CfDialogAction
import net.chronoflow.core.designsystem.system.CfLinearProgress
import net.chronoflow.core.designsystem.system.CfMediumTopAppBar
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.designsystem.system.CfSegmentedButtons
import net.chronoflow.core.designsystem.system.CfSnackbarHost
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.CfTimePickerSheet
import net.chronoflow.core.designsystem.system.CfTopAppBar
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.ui.R
import java.util.Locale

enum class GallerySection {
    Buttons,
    Fields,
    CardsRows,
    PillsBadges,
    BannersNotices,
    Skeleton,
    Bars,
    Controls,
    Sheet,
    Alert,
    Snackbar,
    DatePicker,
    TimePicker,
}

@Composable
fun GalleryHost(
    language: AppLanguage,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val base = LocalContext.current
    val configuration = LocalConfiguration.current
    val context =
        remember(base, configuration, language) {
            base.createConfigurationContext(
                Configuration(configuration).apply { setLocale(Locale.forLanguageTag(language.tag)) },
            )
        }
    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides context.resources.configuration,
    ) {
        ChronoFlowTheme(darkTheme = dark) { content() }
    }
}

@Composable
fun GalleryPage(
    section: GallerySection,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CfTheme.colors.background)
            .padding(LocalSafeInsets.current.asPaddingValues())
            .padding(horizontal = PlanUITokens.space4.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        when (section) {
            GallerySection.Buttons -> Buttons()
            GallerySection.Fields -> Fields()
            GallerySection.CardsRows -> CardsRows()
            GallerySection.PillsBadges -> PillsBadges()
            GallerySection.BannersNotices -> BannersNotices()
            GallerySection.Skeleton -> Skeleton()
            GallerySection.Bars -> Bars()
            GallerySection.Controls -> Controls()
            GallerySection.Sheet -> Sheet()
            GallerySection.Alert -> Alert()
            GallerySection.Snackbar -> Snackbar()
            GallerySection.DatePicker -> DatePicker()
            GallerySection.TimePicker -> TimePicker()
        }
        content()
    }
}

@Composable fun ColumnScope.Buttons() {
    CfButtonStyle.entries.forEach { style ->
        CfButton(stringResource(R.string.screens_mark_as_done), {
        }, style = style, size = CfButtonSize.Lg, leadingIcon = LucideGlyphs.Check.imageVector(2.5f))
    }
    CfButton(stringResource(R.string.screens_skip), {}, size = CfButtonSize.Sm)
    CfButton(stringResource(R.string.screens_undo), {}, size = CfButtonSize.Touch, enabled = false)
    CfButton(stringResource(R.string.auth_sign_in), {
    }, loading = true, loadingDescription = stringResource(R.string.settings_saved))
    CfSpinner(color = CfTheme.colors.foreground)
    CfIconButton(LucideGlyphs.Ellipsis.imageVector(), stringResource(R.string.auth_back), {})
    CfButton(
        stringResource(R.string.welcome_plan_my_trip),
        {},
        style = CfButtonStyle.Dawn,
        fillWidth = true,
        minHeight = WelcomeTokens.buttonHeight.dp,
        textStyle = CfTheme.type.primaryButton,
        contentPadding =
            PaddingValues(
                horizontal = WelcomeTokens.cardHorizontalPadding.dp,
                vertical = WelcomeTokens.brandGap.dp,
            ),
    )
    CfButton(
        stringResource(R.string.welcome_sign_in),
        {},
        style = CfButtonStyle.OnSky,
        fillWidth = true,
        minHeight = WelcomeTokens.buttonHeight.dp,
        textStyle = CfTheme.type.secondaryButton,
        contentPadding =
            PaddingValues(
                horizontal = WelcomeTokens.cardHorizontalPadding.dp,
                vertical = WelcomeTokens.brandGap.dp,
            ),
    )
}

@Composable fun ColumnScope.Fields() {
    val focus = remember { FocusRequester() }
    CfTextField(remember { TextFieldState() }, stringResource(R.string.auth_email), testTag = "gallery.email.default")
    CfTextField(
        remember { TextFieldState() },
        stringResource(R.string.auth_email),
        Modifier.focusRequester(focus),
        testTag = "gallery.email.focused",
        keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false),
    )
    CfTextField(
        remember { TextFieldState() },
        stringResource(R.string.auth_email),
        testTag = "gallery.email.placeholder",
        placeholder = stringResource(R.string.auth_email),
    )
    CfTextField(
        remember { TextFieldState() },
        stringResource(R.string.auth_email),
        testTag = "gallery.email.hint",
        hint = stringResource(R.string.auth_enter_your_email_and_we_ll_send_you_bf365b),
    )
    LaunchedEffect(focus) { focus.requestFocus() }
    CfTextField(
        remember { TextFieldState() },
        stringResource(R.string.auth_email),
        testTag = "gallery.email",
        error = stringResource(R.string.auth_enter_a_valid_email_address_like_nam_166a13),
    )
    CfSecureTextField(
        remember { TextFieldState() },
        stringResource(R.string.auth_password),
        testTag = "gallery.password",
        error = stringResource(R.string.auth_check_both_and_try_again_or_reset_yo_447280),
    )
}

@Composable fun ColumnScope.CardsRows() {
    CfCard(rows = true) {
        CfRow(
            stringResource(R.string.settings_two_factor_authentication),
            subtitle = stringResource(R.string.settings_when_it_s_on_you_sign_in_with_yo_bb939f),
            icon = LucideGlyphs.ShieldCheck.imageVector(),
            badge = stringResource(R.string.settings_off),
            lock = true,
            onClick = {},
        )
        CfDivider()
        CfRow(
            stringResource(
                R.string.settings_add_a_passkey,
            ),
            enabled = false,
            icon = LucideGlyphs.Lock.imageVector(),
            onClick = {
            },
        )
    }
    CfCard {
        Text(
            stringResource(R.string.settings_saved),
            color = CfTheme.colors.cardForeground,
            style = CfTheme.type.authBody,
        )
    }
}

@Composable fun ColumnScope.PillsBadges() {
    val options =
        listOf(
            CfPillOption("light", stringResource(R.string.settings_light)),
            CfPillOption("dark", stringResource(R.string.settings_dark)),
            CfPillOption("system", stringResource(R.string.settings_system)),
        )
    var selected by remember { mutableStateOf("light") }
    CfPillGroup(options, selected, { selected = it }, testTag = "gallery.pills")
    CfPill(stringResource(R.string.settings_system), false, {})
    Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        CfBadge(stringResource(R.string.screens_now), variant = CfBadgeVariant.Dawn)
        CfBadge(stringResource(R.string.screens_skipped), variant = CfBadgeVariant.Outline)
        CfBadge(stringResource(R.string.screens_done))
    }
}

@Composable fun ColumnScope.BannersNotices() {
    CfBanner(
        stringResource(R.string.auth_that_email_and_password_don_t_match),
        stringResource(R.string.auth_check_both_and_try_again_or_reset_yo_447280),
        actionLabel = stringResource(R.string.auth_try_again),
        onAction = {
        },
        testTag = "gallery.banner",
        actionTestTag = "gallery.banner.action",
    )
    CfBanner(
        stringResource(R.string.auth_something_went_wrong_on_our_side),
        stringResource(R.string.auth_try_again_in_a_moment),
    )
    CfNoticeTone.entries.forEach { tone ->
        CfNotice(
            stringResource(R.string.reminders_this_device_can_t_show_reminders),
            stringResource(R.string.reminders_you_can_still_follow_every_step_6dddec),
            tone = tone,
        )
    }
}

@Composable fun ColumnScope.Skeleton() {
    CfSkeleton(Modifier.fillMaxWidth(.6f).height(PlanUITokens.space6.dp), CfTheme.shapes.sm)
    CfSkeleton(Modifier.fillMaxWidth(.4f).height(PlanUITokens.radiusLg.dp), CfTheme.shapes.full)
    Row(
        Modifier.height(PlanUITokens.space14.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        CfSkeleton(
            Modifier.size(PlanUITokens.space5.dp),
            androidx.compose.foundation.shape
                .RoundedCornerShape(AuthTokens.brandGap.dp),
        )
        CfSkeleton(Modifier.width(90.dp).height(PlanUITokens.radiusLg.dp), CfTheme.shapes.full)
    }
    CfSkeleton(Modifier.size(40.dp))
    CfSkeleton(Modifier.width(80.dp).height(20.dp), CfTheme.shapes.full)
    SettingsTokens.skeletonRowWidths.forEach { width ->
        Row(
            Modifier.height(SettingsTokens.counterHeight.dp),
            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        ) {
            CfSkeleton(Modifier.size(SettingsTokens.rowDisc.dp), CfTheme.shapes.full)
            CfSkeleton(Modifier.width(width.dp).height(PlanUITokens.space4.dp), CfTheme.shapes.full)
        }
    }
}

@Composable fun ColumnScope.Bars() {
    val title = stringResource(R.string.settings_change_password)
    val back = stringResource(R.string.auth_back)
    CfMediumTopAppBar(title, TopAppBarDefaults.exitUntilCollapsedScrollBehavior(), navigationIcon = {
        CfBackButton(back, {})
    })
    CfTopAppBar(title, navigationIcon = { CfBackButton(back, {}) })
}

@Composable fun ColumnScope.Controls() {
    var checked by remember { mutableStateOf(true) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = PlanUITokens.actionMinHeight.dp)
            .cfToggleable(checked, {
                checked = it
            })
            .testTag("gallery.toggle"),
    ) {
        Text(stringResource(R.string.settings_light), Modifier.weight(1f), color = CfTheme.colors.foreground)
        CfSwitch(checked, null)
    }
    CfSwitch(false, {}, Modifier.testTag("gallery.switch"))
    CfSegmentedButtons(
        listOf(
            stringResource(R.string.settings_light),
            stringResource(R.string.settings_dark),
            stringResource(R.string.settings_system),
        ),
        0,
        {
        },
        testTag = "gallery.segments",
    )
    CfLinearProgress({ .5f })
    CfLinearProgress()
    CfPullToRefreshBox(
        true,
        {},
        Modifier.fillMaxWidth().height(PlanUITokens.space16.dp),
    ) { Box(Modifier.fillMaxSize()) }
}

@Composable fun ColumnScope.Sheet() {
    val saved = stringResource(R.string.settings_saved)
    CfModalBottomSheet(onDismissRequest = {}, modifier = Modifier.testTag("gallery.sheet")) {
        Text(
            saved,
            Modifier.padding(PlanUITokens.space5.dp).testTag("gallery.sheet.content"),
        )
    }
}

@Composable fun ColumnScope.Alert() {
    CfAlertDialog(
        stringResource(R.string.settings_change_password),
        stringResource(R.string.settings_we_couldn_t_save_your_change_ple_354da2),
        CfDialogAction(stringResource(R.string.settings_done), {
        }),
        {},
        CfDialogAction(stringResource(R.string.settings_cancel), {}),
    )
}

@Composable fun ColumnScope.Snackbar() {
    val host = remember { SnackbarHostState() }
    val message = stringResource(R.string.settings_we_couldn_t_save_your_change_ple_354da2)
    val action = stringResource(R.string.auth_try_again)
    LaunchedEffect(host) { host.showSnackbar(message, action, duration = SnackbarDuration.Indefinite) }
    CfSnackbarHost(host, Modifier.testTag("gallery.snackbar"))
}

@Composable fun ColumnScope.DatePicker() {
    CfDatePickerDialog(rememberDatePickerState(initialSelectedDateMillis = 1794873600000L), {
    }, {}, stringResource(R.string.settings_done), stringResource(R.string.settings_cancel))
}

@Composable fun ColumnScope.TimePicker() {
    CfTimePickerSheet(
        title = stringResource(R.string.settings_bedtime),
        subtitle = stringResource(R.string.settings_on_a_normal_work_or_school_day),
        footnote = null,
        initialHour = 22,
        initialMinute = 30,
        is24Hour = true,
        confirmLabel = stringResource(R.string.settings_done),
        dismissLabel = stringResource(R.string.settings_cancel),
        onConfirm = { _, _, _ -> },
        onDismiss = {},
    )
}
