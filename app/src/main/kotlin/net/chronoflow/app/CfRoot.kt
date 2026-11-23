package net.chronoflow.app

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Deferred
import net.chronoflow.app.auth.SignInViewModelFactory
import net.chronoflow.app.auth.SignUpViewModelFactory
import net.chronoflow.app.auth.TwoFactorViewModelFactory
import net.chronoflow.app.onboarding.WalkthroughEntry
import net.chronoflow.app.onboarding.onboardingExtras
import net.chronoflow.app.shell.AppShell
import net.chronoflow.core.designsystem.effect.skyBackground
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.config.resolve
import net.chronoflow.core.navigation.RootState
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.ui.window.LocalWindowSize
import net.chronoflow.feature.auth.forgot.ForgotPasswordRoute
import net.chronoflow.feature.auth.reset.ResetPasswordRoute
import net.chronoflow.feature.auth.shell.EmailAppLauncher
import net.chronoflow.feature.auth.signin.SignInScreen
import net.chronoflow.feature.auth.signup.CustomTabPolicyOpener
import net.chronoflow.feature.auth.signup.SignUpRoute
import net.chronoflow.feature.auth.twofactor.TwoFactorScreen
import net.chronoflow.feature.auth.verify.VerifyEmailRoute
import net.chronoflow.feature.launch.LaunchOverlay
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.welcome.WelcomeScreen
import net.chronoflow.feature.onboarding.terms.TermsRoute
import net.chronoflow.feature.onboarding.terms.TermsViewModel
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger
import net.chronoflow.feature.trip.timeline.TimelineJankTagger

@Composable
internal fun CfRoot(
    graph: AppGraph,
    claimed: Boolean,
    reduceMotion: Boolean,
    firstCommit: Deferred<Unit>,
    jankTagger: TimelineJankTagger?,
    destinations: WelcomeDestinations = graph.welcomeDestinations,
    onSky: Boolean = false,
    onSystemBars: (Boolean, Boolean, Boolean) -> Unit = { _, _, _ -> },
    initialTheme: ThemeChoice = ThemeChoice.System,
    content: (@Composable () -> Unit)? = null,
) {
    val reminderLanguage =
        LocalResources.current.configuration.locales[0]
            .language
    LaunchedEffect(graph, reminderLanguage) { graph.reminderService.reschedule() }
    val root by graph.rootNavigator.state.collectAsState()
    val displayedRoute = root.route ?: graph.launch.route?.toRoute()
    val activity = LocalActivity.current
    val onRootBack: () -> Unit = { activity?.finish() }
    val theme = rememberDeviceTheme(initialTheme) { graph.deviceAppearance.theme }
    val night = theme.resolve(isSystemInDarkTheme())
    RootThemeContent(night, graph.rootThemeInputs(reduceMotion, firstCommit)) {
        val overlayShowing = graph.launch.overlayActive
        // Skipped overlays wait for first commit before readiness can select a route.
        val launchSky =
            content == null &&
                (
                    displayedRoute == null || displayedRoute == Route.Welcome ||
                        (
                            displayedRoute == Route.SignIn || displayedRoute == Route.TwoFactor ||
                                displayedRoute == Route.Forgot || displayedRoute is Route.Reset ||
                                displayedRoute == Route.SignUp || displayedRoute == Route.Verify
                        )
                )
        rememberDeviceNight(theme, isSystemInDarkTheme(), overlayShowing, onSky || launchSky, onSystemBars)
        LaunchedEffect(graph, overlayShowing, displayedRoute) {
            if (!overlayShowing && displayedRoute != null) graph.finishLaunch()
        }
        if (displayedRoute == Route.Welcome && graph.launch.handOffComplete) {
            val update by graph.controller.updates.collectAsState()
            val intro = graph.launch.launchIntro
            LaunchedEffect(graph, root.revision, update?.epoch, intro) {
                graph.guestRecoveryScheduler.recover(true, intro)
            }
        }
        val currentRoute = rememberUpdatedState(displayedRoute)
        // Replacing the predicate disposes its reporter and can report before the overlay exits.
        ReportDrawnWhen(remember(graph) { { !graph.launch.overlayActive && currentRoute.value != null } })
        val window = LocalWindowInfo.current.containerSize
        val windowSize = with(LocalDensity.current) { DpSize(window.width.toDp(), window.height.toDp()) }
        CompositionLocalProvider(
            LocalSafeInsets provides WindowInsets.safeDrawing,
            LocalWindowSize provides windowSize,
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalTimelineJankTagger provides jankTagger,
        ) {
            var contentReady by remember { mutableStateOf(!claimed) }
            LaunchedEffect(Unit) {
                withFrameNanos { }
                contentReady = true
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(CfTheme.colors.background)
                    .semantics { testTagsAsResourceId = true }
                    .testTag("app.root"),
            ) {
                if (contentReady) {
                    Box(
                        if (overlayShowing) Modifier.semantics { hideFromAccessibility() } else Modifier,
                    ) {
                        if (content !=
                            null
                        ) {
                            content()
                        } else {
                            RootContent(
                                RootState(displayedRoute, root.revision),
                                onBack = onRootBack,
                            ) { route ->
                                when (route) {
                                    Route.Welcome -> {
                                        if (!overlayShowing || graph.launch.fontsReady) {
                                            WelcomeScreen(
                                                graph.launch.launchIntro,
                                                graph.launch.moonAnchorStore,
                                                destinations::planMyTrip,
                                                destinations::signIn,
                                                modifier = Modifier.testTag("route.welcome"),
                                            )
                                        }
                                    }

                                    is Route.Reset -> {
                                        val model = graph.auth.resetModel
                                        if (model == null) {
                                            Box(Modifier.fillMaxSize().skyBackground(CfTheme.colors.isDark))
                                            LaunchedEffect(Unit) { graph.rootNavigator.navigate(Route.SignIn) }
                                        } else {
                                            val context = LocalContext.current
                                            val launcher = remember(context) { EmailAppLauncher(context) }
                                            ResetPasswordRoute(model, launcher::open)
                                        }
                                    }

                                    Route.Forgot -> {
                                        val model =
                                            remember(graph.auth) {
                                                graph.auth.forgotModel
                                                    ?: graph.auth.startForgot()
                                            }
                                        val context = LocalContext.current
                                        val launcher = remember(context) { EmailAppLauncher(context) }
                                        ForgotPasswordRoute(model, expired = false, openEmailApp = launcher::open)
                                    }

                                    Route.TwoFactor -> {
                                        val extras =
                                            remember(graph) {
                                                MutableCreationExtras().apply { set(AppGraphKey, graph) }
                                            }
                                        TwoFactorScreen(viewModel(factory = TwoFactorViewModelFactory, extras = extras))
                                    }

                                    Route.Verify -> {
                                        val coordinator = graph.auth.verification
                                        val model by coordinator.model.collectAsStateWithLifecycle()
                                        LaunchedEffect(coordinator) { coordinator.prepareVerification() }
                                        val current = model
                                        if (current == null) {
                                            Box(Modifier.fillMaxSize().skyBackground(CfTheme.colors.isDark))
                                        } else {
                                            val context = LocalContext.current
                                            val launcher = remember(context) { EmailAppLauncher(context) }
                                            androidx.compose.runtime.key(
                                                current,
                                            ) { VerifyEmailRoute(current, launcher::open) }
                                        }
                                    }

                                    Route.SignUp -> {
                                        val extras =
                                            remember(
                                                graph,
                                            ) { MutableCreationExtras().apply { set(AppGraphKey, graph) } }
                                        val context = LocalContext.current
                                        val opener = remember(context, night) { CustomTabPolicyOpener(context, night) }
                                        SignUpRoute(
                                            viewModel(factory = SignUpViewModelFactory, extras = extras),
                                            opener,
                                        )
                                    }

                                    Route.GuestWizard -> {
                                        net.chronoflow.app.guest
                                            .GuestWizardRoute(graph, root.revision)
                                    }

                                    Route.Terms -> {
                                        TermsRoute(
                                            viewModel(
                                                factory = TermsViewModel.Factory,
                                                extras = onboardingExtras(graph),
                                            ),
                                            signOutConfirmation = { count, onStay, onDiscard ->
                                                val current by graph.sessionGraphs.current.collectAsStateWithLifecycle()
                                                current?.let { session ->
                                                    val outbox = remember(session) { session.outbox.values() }
                                                    net.chronoflow.app.settings.TermsSignOutConfirmation(
                                                        count,
                                                        outbox,
                                                        onStay,
                                                        onDiscard,
                                                    )
                                                }
                                            },
                                        )
                                    }

                                    is Route.Walkthrough -> {
                                        LaunchedEffect(route.replay) {
                                            if (route.replay && BuildConfig.DEBUG) {
                                                android.util.Log.w("ChronoFlow", "root walkthrough replay")
                                            }
                                        }
                                        WalkthroughEntry(graph, route.replay) {
                                            graph.rootNavigator.navigate(Route.Settings)
                                        }
                                    }

                                    Route.SignIn -> {
                                        val extras =
                                            remember(
                                                graph,
                                            ) { MutableCreationExtras().apply { set(AppGraphKey, graph) } }
                                        SignInScreen(
                                            viewModel(factory = SignInViewModelFactory, extras = extras),
                                            graph.auth.router,
                                        )
                                    }

                                    Route.Today, Route.Trips, Route.Settings, is Route.TripPage, is Route.Wizard -> {
                                        AppShell(route, onRootBack) { RoutePlaceholder(it, LaunchPlatform.routeLabel) }
                                    }

                                    else -> {
                                        RoutePlaceholder(route, LaunchPlatform.routeLabel)
                                    }
                                }
                            }
                        }
                    }
                }
                if (overlayShowing) LaunchOverlay(graph.launch)
            }
        }
    }
}
