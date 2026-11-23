package net.chronoflow.core.designsystem

import androidx.compose.ui.graphics.vector.PathParser
import net.chronoflow.core.designsystem.icon.LucideGlyph
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LucideGlyphsTest {
    @Test fun everyBoardPathParsesAndVectorsAreCached() {
        val glyphs =
            LucideGlyphs::class.java.methods
                .filter {
                    it.returnType == LucideGlyph::class.java
                }.map { it.invoke(LucideGlyphs) as LucideGlyph }
        assertEquals(50, glyphs.size)
        glyphs.forEach { glyph -> glyph.paths.forEach { PathParser().parsePathString(it.d) } }
        assertEquals(
            "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z",
            LucideGlyphs.Moon.paths
                .single()
                .d,
        )
        assertEquals(
            "m15 18-6-6 6-6",
            LucideGlyphs.ChevronLeft.paths
                .single()
                .d,
        )
        assertEquals(5, LucideGlyphs.FileText.paths.size)
        assertEquals(2, LucideGlyphs.KeyRound.paths.size)
        assertTrue(LucideGlyphs.KeyRound.paths[1].filled)
        assertTrue(
            LucideGlyphs.Star.paths
                .single()
                .filled,
        )
        assertEquals(2, LucideGlyphs.Clock.paths.size)
        assertEquals("M12 6v6l4 2", LucideGlyphs.Clock.paths[1].d)
        assertSame(LucideGlyphs.Moon.imageVector(), LucideGlyphs.Moon.imageVector())
    }
}
