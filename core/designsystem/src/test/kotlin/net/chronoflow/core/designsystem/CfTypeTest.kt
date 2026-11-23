package net.chronoflow.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.generated.CfFace
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.CfTypeSpec
import net.chronoflow.core.designsystem.theme.CfType
import net.chronoflow.core.designsystem.theme.Figtree
import net.chronoflow.core.designsystem.theme.Fraunces20
import net.chronoflow.core.designsystem.theme.Fraunces48
import net.chronoflow.core.designsystem.theme.fontFamily
import net.chronoflow.core.designsystem.theme.fontWeight
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.designsystem.theme.toTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CfTypeTest {
    @Test fun wizardPickerUsesThePlatformFaceAndBoardMetrics() {
        val picker = CfTypeScale.wizardPicker.toTextStyle()
        assertSame(FontFamily.Default, picker.fontFamily)
        assertEquals(FontWeight.W400, picker.fontWeight)
        assertEquals(17.sp, picker.fontSize)
        assertEquals(22.sp, picker.lineHeight)
        assertEquals((-0.01).em, picker.letterSpacing)
    }

    @Test fun facesAndStylesKeepTheIosMetrics() {
        assertSame(Fraunces48, CfFace.Fraunces400Opsz48.fontFamily())
        assertEquals(FontWeight.W500, CfFace.Fraunces500Opsz20.fontWeight())
        val label = CfTypeScale.authLabel.toTextStyle()
        assertSame(Figtree, label.fontFamily)
        assertEquals(FontWeight.W500, label.fontWeight)
        assertEquals(14.sp, label.fontSize)
        assertEquals(20.sp, label.lineHeight)
        assertEquals(0.em, label.letterSpacing)
        assertEquals(TextUnit.Unspecified, CfTypeSpec(CfFace.Figtree400, 16f, null, 0f).toTextStyle().lineHeight)
        val type = CfType()
        assertSame(Fraunces20, type.planDayTitle.fontFamily)
        assertEquals(26.sp, type.planDayTitle.lineHeight)
        assertSame(type.authLabel, type.style(CfTypeScale.authLabel))
        assertEquals("tnum", TextStyle.Default.tnum().fontFeatureSettings)
    }
}
