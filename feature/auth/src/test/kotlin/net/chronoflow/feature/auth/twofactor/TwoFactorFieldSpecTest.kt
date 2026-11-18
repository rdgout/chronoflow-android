package net.chronoflow.feature.auth.twofactor

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.TwoFactorModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * ContentType uses identity equality and its public toString omits hints in Compose UI 1.12.1.
 * Check the shared production value's wiring; hint-level assertions would require internal API.
 */
class TwoFactorFieldSpecTest {
    @Test fun codeUsesNumberKeyboardAndOneTimeCodeAutofill() {
        val spec = TwoFactorFieldSpec.forMode(TwoFactorModel.Mode.Code)
        assertEquals(KeyboardType.Number, spec.keyboardOptions.keyboardType)
        assertEquals(ImeAction.Go, spec.keyboardOptions.imeAction)
        assertEquals(KeyboardCapitalization.None, spec.keyboardOptions.capitalization)
        assertEquals(false, spec.keyboardOptions.autoCorrectEnabled)
        assertSame(TwoFactorFieldSpec.CodeContentType, spec.contentType)
        assertEquals(R.string.auth_6_digit_code, spec.label)
        assertNull(spec.hint)
        assertNull(spec.placeholder)
        assertEquals("two-factor.code", spec.tag)
    }

    @Test fun recoveryUsesAsciiAndNoAutofill() {
        val spec = TwoFactorFieldSpec.forMode(TwoFactorModel.Mode.Recovery)
        assertEquals(KeyboardType.Ascii, spec.keyboardOptions.keyboardType)
        assertEquals(ImeAction.Go, spec.keyboardOptions.imeAction)
        assertEquals(KeyboardCapitalization.None, spec.keyboardOptions.capitalization)
        assertEquals(false, spec.keyboardOptions.autoCorrectEnabled)
        assertNull(spec.contentType)
        assertEquals(R.string.auth_recovery_code, spec.label)
        assertEquals(R.string.auth_each_recovery_code_works_once, spec.hint)
        assertEquals(R.string.auth_xxxx_xxxx, spec.placeholder)
        assertEquals("two-factor.recovery_code", spec.tag)
    }

    @Test fun filteringIsAsciiBeforeTruncation() {
        assertEquals("123456", TwoFactorModel.filterCode("a12 ٣b345678"))
    }

    @Test fun fullWidthDigitsAreRejected() {
        assertEquals("", TwoFactorModel.filterCode("１２３"))
    }

    @Test fun excessDigitsAreTruncated() {
        assertEquals("987654", TwoFactorModel.filterCode("9876543"))
    }

    @Test fun emptyStaysEmpty() {
        assertEquals("", TwoFactorModel.filterCode(""))
    }
}
