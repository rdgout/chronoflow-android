package net.chronoflow.feature.auth.shell

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class AuthLayoutTests {
    @RunWith(Parameterized::class)
    class Content(
        private val fixture: AuthContentFixture,
    ) {
        private val name get() = fixture.name
        private val size get() = fixture.size
        private val insets get() = fixture.insets

        @Test fun zeroInsetBoardAndEveryDeviceRespectFourInsets() {
            val rect = AuthLayout.content(size, insets)
            assertEquals(insets.left + 16f, rect.left, 0f)
            assertEquals(size.width - insets.right - 16f, rect.right, 0f)
            assertEquals(insets.top + 16f, rect.top, 0f)
            assertEquals(size.height - insets.bottom - 24f, rect.bottom, 0f)
            if (name == "board") assertEquals(Rect(16f, 16f, 359f, 788f), rect)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases(): List<AuthContentFixture> =
                listOf(AuthContentFixture("board", Size(375f, 812f), AuthLayout.Insets(0f, 0f, 0f, 0f))) +
                    DeviceProfiles.all.flatMap { profile ->
                        val size = Size(profile.widthDp.toFloat(), profile.heightDp.toFloat())
                        listOf(
                            AuthContentFixture(
                                profile.name,
                                size,
                                AuthLayout.Insets(
                                    profile.insets.top,
                                    profile.insets.left,
                                    profile.insets.bottom,
                                    profile.insets.right,
                                ),
                            ),
                            AuthContentFixture(
                                profile.name + "-asymmetric",
                                size,
                                AuthLayout.Insets(0f, 24f, 20f, 62f),
                            ),
                        )
                    }
        }
    }

    class Reveal {
        @Test fun measuredKeyboardViewportRevealsPairOrReportsOverflow() {
            val field = Rect(20f, 300f, 320f, 400f)
            val submit = Rect(20f, 500f, 320f, 544f)
            val viewport = Rect(0f, 0f, 375f, 330f)
            val result = AuthLayout.reveal(field, submit, viewport)
            assertTrue(result.fits)
            assertTrue(field.top - result.offset >= viewport.top)
            assertTrue(field.bottom - result.offset <= viewport.bottom)
            assertTrue(submit.top - result.offset >= viewport.top)
            assertTrue(submit.bottom - result.offset <= viewport.bottom)
            val overflow = AuthLayout.reveal(field, submit, Rect(0f, 0f, 375f, 150f))
            assertFalse(overflow.fits)
            assertEquals(0f, field.top - overflow.offset, 0f)
            assertEquals(0f, AuthLayout.reveal(field, submit, Rect(0f, 250f, 375f, 600f)).offset, 0f)
        }
    }
}

class AuthContentFixture(
    val name: String,
    val size: Size,
    val insets: AuthLayout.Insets,
) {
    override fun toString(): String = name
}
