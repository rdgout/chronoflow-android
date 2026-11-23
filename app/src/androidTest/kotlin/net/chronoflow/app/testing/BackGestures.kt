package net.chronoflow.app.testing

import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue

internal fun UiDevice.predictiveBack() {
    val y = displayHeight / 2
    assertTrue("predictive back gesture was not injected", swipe(1, y, displayWidth * 3 / 4, y, 20))
}
