package net.chronoflow.core.testing

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecreatedSheetCheckTest {
    @Test fun activeSheetHidesTheUnderlyingPageUntilDismissed() {
        val windows = Windows()
        windows.verify()
        assertTrue(windows.sheetChecked)
        assertTrue(windows.pageChecked)
    }

    @Test fun processDeathFailsWhileTheRestoredSheetIsStillVisible() {
        val windows = Windows(sameProcess = false)
        assertThrows(IllegalStateException::class.java) { windows.verify() }
        assertTrue(windows.sheetChecked)
        assertTrue(windows.sheetVisible)
    }

    @Test fun missingUnderlyingPageStillFailsAfterDismissal() {
        val windows = Windows(pageExists = false)
        assertThrows(IllegalStateException::class.java) { windows.verify() }
        assertTrue(windows.sheetChecked)
    }

    private class Windows(
        val sameProcess: Boolean = true,
        val pageExists: Boolean = true,
    ) {
        var sheetVisible = true
        var sheetChecked = false
        var pageChecked = false

        fun verify() =
            verifyRecreatedSheet(
                requireSheet = {
                    check(sheetVisible)
                    sheetChecked = true
                },
                requireSameProcess = { check(sameProcess) },
                dismissSheet = { sheetVisible = false },
                requireSheetGone = { check(!sheetVisible) },
                requireUnderlyingPage = {
                    check(!sheetVisible) { "the modal sheet owns the active window" }
                    check(pageExists) { "underlying page is missing" }
                    pageChecked = true
                },
            )
    }
}
