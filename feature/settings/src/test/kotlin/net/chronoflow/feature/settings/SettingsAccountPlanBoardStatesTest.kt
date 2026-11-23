package net.chronoflow.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAccountPlanBoardStatesTest {
    @Test fun creditsFixtureKeepsThePendingCheckout() {
        val state = SettingsAccountPlanBoardStates.plan("PlanPending")
        assertTrue(state.paymentPending)
        assertEquals(2, state.credits)
        assertTrue(state.loaded)
        assertFalse(state.unlimited)
    }

    @Test fun noLinkFixtureHasTheBalanceWithoutTheProcessingNotice() {
        val state = SettingsAccountPlanBoardStates.plan("PlanCredits")
        assertFalse(state.paymentPending)
        assertEquals(2, state.credits)
        assertTrue(state.hasBalance)
    }
}
