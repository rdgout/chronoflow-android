package net.chronoflow.core.testing.audit

data class AuditScreen(
    val name: String,
    val seeds: Map<String, String>,
    val contentTag: String,
) {
    override fun toString(): String = name
}

private val auditAccount =
    mapOf(
        "CF_AUTH_TEST_SIGNED_IN" to "demo",
        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
        "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
    )
private val auditTrip = auditAccount + ("CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000001")

val auditScreens: List<AuditScreen> =
    listOf(
        AuditScreen("Main", emptyMap(), "welcome.plan"),
        AuditScreen(
            "Walkthrough",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_WALKTHROUGH_TEST_PENDING" to "1",
            ),
            "walkthrough.screen",
        ),
        AuditScreen("TermsUpdate", auditAccount + ("CF_AUTH_TEST_TERMS_PENDING" to "1"), "terms.screen"),
        AuditScreen("Plan", auditTrip, "trip.page"),
        AuditScreen(
            "PlanList",
            auditAccount + ("CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000001:list"),
            "trip.mode.list",
        ),
        AuditScreen("PlanEnd", auditTrip + ("CF_PLAN_TEST_POSITION" to "summary"), "trip.summary"),
        AuditScreen("TripActions", auditTrip + ("CF_WIZARD_BOARD_CAPTURE" to "TripActions"), "trip.actions"),
        AuditScreen("TodayEmpty", auditAccount + ("CF_REFRESH_TEST_EMPTY_TRIPS" to "1"), "today.empty"),
        AuditScreen(
            "TodayFuture",
            auditAccount +
                mapOf(
                    "CF_AUTH_TEST_AUDIT_SCREEN" to "today",
                    "CF_AUTH_TEST_AUDIT_STATE" to "future",
                ),
            "today.future",
        ),
        AuditScreen(
            "TodayCompleted",
            auditAccount +
                mapOf(
                    "CF_AUTH_TEST_AUDIT_SCREEN" to "today",
                    "CF_AUTH_TEST_AUDIT_STATE" to "completed",
                ),
            "today.completed",
        ),
        AuditScreen(
            "GuestSignup",
            mapOf(
                "CF_GUEST_WIZARD" to "1",
                "CF_WIZARD_BOARD_CAPTURE" to "GuestSignup",
            ),
            "guest-signup.name",
        ),
    ) +
        listOf(
            AuditScreen(
                "SetHome",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "none",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.screen",
            ),
            AuditScreen(
                "SetAccount",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "account",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.account.screen",
            ),
            AuditScreen(
                "SetProfile",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "profile",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.profile.screen",
            ),
            AuditScreen(
                "SetLanguage",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "language",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.language.screen",
            ),
            AuditScreen(
                "SetSecurity",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "security",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.security.screen",
            ),
            AuditScreen(
                "SetPassword",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "password",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.password.screen",
            ),
            AuditScreen(
                "Set2FA",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "twoFactor",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.twoFactor.screen",
            ),
            AuditScreen(
                "Set2FASetup",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "twoFactorSetup",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.twoFactor.screen",
            ),
            AuditScreen(
                "Set2FAManage",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "twoFactorSettings",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ) + ("CF_AUTH_TEST_SIGNED_IN" to "twofactor"),
                "settings.twoFactor.screen",
            ),
            AuditScreen(
                "SetPasskeys",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "passkeys",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.passkeys.screen",
            ),
            AuditScreen(
                "SetBodyClock",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "bodyClock",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.bodyClock.screen",
            ),
            AuditScreen(
                "SetPreferences",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "preferences",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.preferences.screen",
            ),
            AuditScreen(
                "SetReminders",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "reminders",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "reminders.screen",
            ),
            AuditScreen(
                "SetPlan",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "plan",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.plan.screen",
            ),
            AuditScreen(
                "SetDelete",
                auditAccount +
                    mapOf(
                        "CF_SETTINGS_ROUTE" to "delete",
                        "CF_AUTH_TEST_AUDIT_SCREEN" to "settings",
                    ),
                "settings.delete.screen",
            ),
        ) +
        listOf(
            "WizardAirportFrom",
            "WizardAirportFromDarkNL",
            "WizardAirportHome",
            "WizardAirportHomeDarkNL",
            "WizardCalculating",
            "WizardCalculatingDarkNL",
            "WizardCheckOffline",
            "WizardCheckOfflineDarkNL",
            "WizardCheckOfflineEdit",
            "WizardCheckOfflineEditDarkNL",
            "WizardCheckServer",
            "WizardCheckServerDarkNL",
            "WizardCheckServerEdit",
            "WizardCheckServerEditDarkNL",
            "WizardCheckThrottle",
            "WizardCheckThrottleDarkNL",
            "WizardChecking",
            "WizardCheckingAgain",
            "WizardCheckingAgainDarkNL",
            "WizardCheckingDark",
            "WizardCheckingEdit",
            "WizardCheckingEditDarkNL",
            "WizardCheckingNL",
            "WizardCheckingSlow",
            "WizardCheckingSlowDarkNL",
            "WizardDiscard",
            "WizardDiscardDarkNL",
            "WizardDiscardEdit",
            "WizardDiscardEditDarkNL",
            "WizardDuplicate",
            "WizardDuplicateDarkNL",
            "WizardEdit",
            "WizardEditChanged",
            "WizardEditChangedDarkNL",
            "WizardEditDarkNL",
            "WizardEditNoReturn",
            "WizardEditNoReturnDarkNL",
            "WizardEditReturn",
            "WizardEditReturnNoCredits",
            "WizardEditReturnNoCreditsDarkNL",
            "WizardEditWeb",
            "WizardEditWebDarkNL",
            "WizardExcluded",
            "WizardExcludedDarkNL",
            "WizardExcludedEdit",
            "WizardExcludedEditDarkNL",
            "WizardExcludedShift",
            "WizardExcludedShiftDarkNL",
            "WizardFineTune",
            "WizardFineTuneBusy",
            "WizardFineTuneBusyDarkNL",
            "WizardFineTuneDark",
            "WizardFineTuneDarkNL",
            "WizardFineTuneMulti",
            "WizardFineTuneMultiDarkNL",
            "WizardFineTuneNL",
            "WizardFineTuneNone",
            "WizardFineTuneNoneDarkNL",
            "WizardFineTuneSheet",
            "WizardFineTuneSheetDarkNL",
            "WizardFineTuneSleep",
            "WizardFineTuneSleepDarkNL",
            "WizardHomeRoute",
            "WizardHomeRouteDarkNL",
            "WizardHomeRouteStop",
            "WizardHomeRouteStopDarkNL",
            "WizardOutbound",
            "WizardOutboundDark",
            "WizardOutboundEmpty",
            "WizardOutboundEmptyDarkNL",
            "WizardOutboundErrors",
            "WizardOutboundErrorsDarkNL",
            "WizardOutboundNL",
            "WizardOutboundOvernight",
            "WizardOutboundOvernightDarkNL",
            "WizardOutboundPast",
            "WizardOutboundPastDarkNL",
            "WizardOutboundStop",
            "WizardOutboundStopDarkNL",
            "WizardOutboundStopEmpty",
            "WizardOutboundStopEmptyDarkNL",
            "WizardOutboundThree",
            "WizardOutboundThreeDarkNL",
            "WizardOutboundTwoErrors",
            "WizardOutboundTwoErrorsDarkNL",
            "WizardQAge",
            "WizardQAgeDarkNL",
            "WizardQShift",
            "WizardQShiftLarge",
            "WizardQShiftNL",
            "WizardReady",
            "WizardReadyAdjusted",
            "WizardReadyAdjustedDarkNL",
            "WizardReadyChecking",
            "WizardReadyCheckingDarkNL",
            "WizardReadyDark",
            "WizardReadyFree",
            "WizardReadyFreeDarkNL",
            "WizardReadyNL",
            "WizardReadyNoCredits",
            "WizardReadyNoCreditsDarkNL",
            "WizardReadyOneFree",
            "WizardReadyOneFreeDarkNL",
            "WizardReadyReturn",
            "WizardReadyReturnDarkNL",
            "WizardReadyUnlimited",
            "WizardReadyUnlimitedDarkNL",
            "WizardReturn",
            "WizardReturnDark",
            "WizardReturnFlight",
            "WizardReturnFlightDarkNL",
            "WizardReturnFlightPast",
            "WizardReturnFlightPastDarkNL",
            "WizardReturnNL",
            "WizardReturnWeb",
            "WizardReturnWebDarkNL",
            "WizardSaveOffline",
            "WizardSaveOfflineDarkNL",
            "WizardSaveServer",
            "WizardSaveServerDarkNL",
            "WizardSheetBlue",
            "WizardSheetBlueDarkNL",
            "WizardSheetBlueError",
            "WizardSheetBlueErrorDarkNL",
            "WizardSheetBusy",
            "WizardSheetBusyDarkNL",
            "WizardSheetCheckout",
            "WizardSheetCheckoutDarkNL",
            "WizardSheetDrinks",
            "WizardSheetDrinksDarkNL",
            "WizardSheetDrinksError",
            "WizardSheetDrinksErrorDarkNL",
            "WizardSheetFreeSleep",
            "WizardSheetFreeSleepDarkNL",
            "WizardSheetLight",
            "WizardSheetLightDarkNL",
            "WizardSheetRoom",
            "WizardSheetRoomDarkNL",
            "WizardSheetWake",
            "WizardSheetWakeDarkNL",
            "WizardSleep",
            "WizardStopSearch",
            "WizardStopSearchDarkNL",
            "WizardStopSearchHome",
            "WizardStopSearchHomeDarkNL",
            "WizardStopSearchSecond",
            "WizardStopSearchSecondDarkNL",
            "WizardWhere",
            "WizardWhereDark",
            "WizardWhereEmpty",
            "WizardWhereEmptyDarkNL",
            "WizardWhereNL",
            "WizardWhereSame",
            "WizardWhereStop",
            "WizardWhereStopDarkNL",
            "WizardWhereStops",
            "WizardWhereStopsDarkNL",
        ).map { auditWizardScreen(it, guest = false) } +
        listOf(
            "WizardCheckingGuest",
            "WizardDiscardGuest",
            "WizardDiscardGuestDarkNL",
            "WizardFineTuneGuest",
            "WizardOutboundGuest",
            "WizardOutboundPastGuest",
            "WizardOutboundPastGuestDarkNL",
            "WizardReadyGuest",
            "WizardReadyGuestDarkNL",
            "WizardReadyGuestFree",
            "WizardReadyGuestFreeDarkNL",
            "WizardWhereGuest",
        ).map { auditWizardScreen(it, guest = true) }

private fun auditWizardScreen(
    name: String,
    guest: Boolean,
): AuditScreen {
    val base =
        name
            .removeSuffix("DarkNL")
            .removeSuffix("Dark")
            .removeSuffix("NL")
            .removeSuffix("Large")
    val tag =
        when {
            base.contains("Discard") -> "cf.dialog.confirm"
            base.contains("Airport") || base.startsWith("WizardStopSearch") -> "wizard.airport-search"
            base == "WizardFineTuneSheet" || base.startsWith("WizardSheet") -> "wizard.answer.scroll"
            base.startsWith("WizardCheck") -> "wizard.checking"
            base.startsWith("WizardCalculating") -> "wizard.calculating"
            base.startsWith("WizardReady") || base.startsWith("WizardSave") -> "wizard.ready"
            base.startsWith("WizardFineTune") -> "wizard.fine-tune"
            base.startsWith("WizardExcluded") -> "wizard.excluded.reasons"
            base.startsWith("WizardSleep") -> "wizard.sleep.habitual_bed"
            else -> "wizard.window"
        }
    return AuditScreen(
        name,
        (if (guest) mapOf("CF_GUEST_WIZARD" to "1") else auditAccount) +
            mapOf("CF_WIZARD_AUTO_OPEN" to "1", "CF_WIZARD_BOARD_CAPTURE" to name),
        tag,
    )
}
