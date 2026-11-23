package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.feature.wizard.ready.ReadyBannerUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadyEditSaveFailureCopyTest {
    @Test fun offlineEditKeepsTheTrip() {
        assertEquals(
            WizardCopy("Connect to the internet and try again. If you close now, your trip stays as it was."),
            ReadyBannerUi.SaveFailed(WizardSaveFailure.Kind.offline, 0, editing = true).body,
        )
    }

    @Test fun serverEditKeepsTheTrip() {
        assertEquals(
            WizardCopy(
                "Something went wrong on our side. Try again in a moment. If you close now, your trip stays as it was.",
            ),
            ReadyBannerUi.SaveFailed(WizardSaveFailure.Kind.server, 0, editing = true).body,
        )
    }

    @Test fun createKeepsTheCreateBodies() {
        assertEquals(
            "Connect to the internet and tap Make my plan again. Keep this screen open: closing discards the trip.",
            ReadyBannerUi.SaveFailed(WizardSaveFailure.Kind.offline, 0).body.key,
        )
        assertEquals(
            "Something went wrong on our side. Tap Make my plan to try again.",
            ReadyBannerUi.SaveFailed(WizardSaveFailure.Kind.server, 0).body.key,
        )
    }
}
