package net.chronoflow.core.data.passkey

import android.app.Activity
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PasskeyAnchorTest {
    @Test fun detachOnlyClearsTheAttachedActivity() {
        val first = Robolectric.buildActivity(Activity::class.java).setup()
        val second = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val anchor = PasskeyAnchor()
            anchor.attach(first.get())
            anchor.detach(second.get())
            assertSame(first.get(), anchor.current())
            anchor.detach(first.get())
            assertNull(anchor.current())
        } finally {
            first.pause().stop().destroy()
            second.pause().stop().destroy()
        }
    }
}
