package net.chronoflow.app.perf

data class PerfJourneyExtras(
    val demoAccount: Boolean,
    val wizard: PerfWizard?,
    val ignored: List<String> = emptyList(),
) {
    companion object {
        val None = PerfJourneyExtras(false, null)
        val keys = listOf("cf.perf-account", "cf.perf-wizard")

        fun parse(extras: Map<String, String>): PerfJourneyExtras {
            val ignored = mutableListOf<String>()
            val account = extras["cf.perf-account"]
            val wizard = extras["cf.perf-wizard"]
            if (account != null && account != "demo") ignored += "cf.perf-account=$account"
            if (wizard != null && wizard != "WizardSaveJourney") ignored += "cf.perf-wizard=$wizard"
            return PerfJourneyExtras(
                account == "demo",
                PerfWizard.WizardSaveJourney.takeIf {
                    wizard ==
                        "WizardSaveJourney"
                },
                ignored,
            )
        }
    }
}

enum class PerfWizard { WizardSaveJourney }

internal object PerfJourneyState {
    @Volatile var current: PerfJourneyExtras = PerfJourneyExtras.None
}
