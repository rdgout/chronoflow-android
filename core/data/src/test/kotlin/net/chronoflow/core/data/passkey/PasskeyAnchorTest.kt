package net.chronoflow.core.data.passkey

import android.app.Activity
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PasskeyAnchorTest {
    @Test fun detachOnlyClearsTheAttachedActivity() {
        val first = Activity()
        val second = Activity()
        val anchor = PasskeyAnchor()
        anchor.attach(first)
        anchor.detach(second)
        assertSame(first, anchor.current())
        anchor.detach(first)
        assertNull(anchor.current())
    }
}
