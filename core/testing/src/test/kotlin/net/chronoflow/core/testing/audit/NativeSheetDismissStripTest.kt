package net.chronoflow.core.testing.audit

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NativeSheetDismissStripTest(
    private val name: String,
    private val evidence: NativeSheetDismissStrip,
    private val expected: Boolean,
) {
    @Test fun allNativeScrimIdentityConditionsAreRequired() {
        assertEquals(name, expected, evidence.matches())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val native = NativeSheetDismissStrip(true, true, false, false, true, true, true)
            return listOf(
                arrayOf("native navigation strip", native, true),
                arrayOf("app window", native.copy(materialSheetWindow = false), false),
                arrayOf("other label", native.copy(nativeDismissLabel = false), false),
                arrayOf("tagged control", native.copy(hasTag = true), false),
                arrayOf("focusable control", native.copy(focusable = true), false),
                arrayOf("not clickable", native.copy(clickable = false), false),
                arrayOf("not full width", native.copy(spansWindowWidth = false), false),
                arrayOf("inside app content", native.copy(insideNavigationInset = false), false),
            )
        }
    }
}
