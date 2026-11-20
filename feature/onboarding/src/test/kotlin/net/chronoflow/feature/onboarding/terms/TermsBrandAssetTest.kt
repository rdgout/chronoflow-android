package net.chronoflow.feature.onboarding.terms

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class TermsBrandAssetTest {
    @Test fun vectorMatchesTheIconMaster() {
        val root =
            generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
                .first { File(it, "settings.gradle.kts").exists() }
        val parser = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
        val source = parser.parse(File(root, "_design/icon/ios/AppIcon-default.svg"))
        val vector = parser.parse(File(root, "feature/onboarding/src/main/res/drawable/terms_brand.xml"))
        val android = "http://schemas.android.com/apk/res/android"

        fun Element.a(name: String) = getAttributeNS(android, name)
        assertEquals("28dp", vector.documentElement.a("width"))
        assertEquals("28dp", vector.documentElement.a("height"))
        assertEquals("1024", vector.documentElement.a("viewportWidth"))
        assertEquals("1024", vector.documentElement.a("viewportHeight"))
        val stops = source.getElementsByTagName("stop")
        val items = vector.getElementsByTagName("item")
        assertEquals(3, items.length)
        repeat(stops.length) { index ->
            val stop = stops.item(index) as Element
            val item = items.item(index) as Element
            assertEquals(stop.getAttribute("offset").toFloat(), item.a("offset").toFloat(), 0f)
            assertEquals("#FF" + stop.getAttribute("stop-color").drop(1).uppercase(), item.a("color"))
        }
        val gradient = vector.getElementsByTagName("gradient").item(0) as Element
        assertEquals("0", gradient.a("startY"))
        assertEquals("1024", gradient.a("endY"))
        val group = vector.getElementsByTagName("group").item(0) as Element
        val transform = (source.getElementsByTagName("g").item(0) as Element).getAttribute("transform")
        val values = Regex("[0-9.]+").findAll(transform).map { it.value.toFloat() }.toList()
        assertEquals(values[0], group.a("translateX").toFloat(), 0f)
        assertEquals(values[1], group.a("translateY").toFloat(), 0f)
        assertEquals(values[2], group.a("scaleX").toFloat(), 0f)
        assertEquals(values[2], group.a("scaleY").toFloat(), 0f)
        val original = source.getElementsByTagName("path").item(0) as Element
        val moon = vector.getElementsByTagName("path").item(1) as Element
        assertEquals(original.getAttribute("d"), moon.a("pathData"))
        assertEquals("#FF" + original.getAttribute("stroke").drop(1).uppercase(), moon.a("strokeColor"))
        assertEquals(original.getAttribute("stroke-width"), moon.a("strokeWidth"))
        assertEquals(original.getAttribute("stroke-linecap"), moon.a("strokeLineCap"))
        assertEquals(original.getAttribute("stroke-linejoin"), moon.a("strokeLineJoin"))
    }
}
