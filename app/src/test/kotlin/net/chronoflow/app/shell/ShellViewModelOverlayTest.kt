package net.chronoflow.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.feature.wizard.shell.WizardExit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellViewModelOverlayTest {
    @Test fun closeWizardKeepsTripsAndEveryStack() {
        val initial = ShellNav.initial(ShellTab.Trips)
        val holder = ShellViewModel(initial)
        holder.push(Route.Wizard(WizardMode.Create))
        holder.closeWizard(WizardExit.Trips)
        assertNull(holder.state.overlay)
        assertEquals(ShellTab.Trips, holder.state.selected)
        assertEquals(initial.stacks, holder.state.stacks)
    }

    @Test fun closeWizardKeepsTodayAndEveryStack() {
        val initial = ShellNav.initial()
        val holder = ShellViewModel(initial)
        holder.push(Route.Wizard(WizardMode.Create))
        holder.closeWizard(WizardExit.Trips)
        assertNull(holder.state.overlay)
        assertEquals(ShellTab.Today, holder.state.selected)
        assertEquals(initial.stacks, holder.state.stacks)
    }

    private class Tracked : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    @Test fun closingClearsTheOverlayStoreBeforeTheNextOpen() {
        val holder = ShellViewModel(ShellNav.initial())
        val factory = viewModelFactory { initializer { Tracked() } }
        holder.push(Route.Wizard(WizardMode.Create))
        val firstOwner = holder.overlayOwner
        val first = ViewModelProvider.create(firstOwner, factory)[Tracked::class.java]
        holder.closeWizard(WizardExit.Trips)
        assertTrue(first.cleared)
        holder.push(Route.Wizard(WizardMode.Create))
        assertNotSame(firstOwner, holder.overlayOwner)
        val next = ViewModelProvider.create(holder.overlayOwner, factory)[Tracked::class.java]
        assertNotSame(first, next)
        holder.closeWizard(WizardExit.Trips)
        assertTrue(next.cleared)
    }
}
