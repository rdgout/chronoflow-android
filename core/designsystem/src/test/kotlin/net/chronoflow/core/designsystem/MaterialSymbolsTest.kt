package net.chronoflow.core.designsystem

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialSymbolsTest {
    @Test fun everySymbolUsesTheNativeViewportAndSize() {
        val vectors =
            MaterialSymbols::class.java.methods
                .filter {
                    it.returnType == ImageVector::class.java
                }.map { it.invoke(MaterialSymbols) as ImageVector }
        assertEquals(13, vectors.size)
        vectors.forEach {
            assertEquals(24.dp, it.defaultWidth)
            assertEquals(24.dp, it.defaultHeight)
            assertEquals(960f, it.viewportWidth)
            assertEquals(960f, it.viewportHeight)
        }
        assertTrue(MaterialSymbols.ArrowBack.autoMirror)
    }
}
