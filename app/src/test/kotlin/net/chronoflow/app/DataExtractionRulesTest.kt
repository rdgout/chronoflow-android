package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.containsAtLeast
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test

class DataExtractionRulesTest {
    private val root = parseXml("src/main/res/xml/data_extraction_rules.xml").documentElement

    @Test
    fun bothSectionsExist() {
        assertThat(root.children("cloud-backup").size).isEqualTo(1)
        assertThat(root.children("device-transfer").size).isEqualTo(1)
    }

    @Test
    fun sectionsHoldNoIncludes() {
        listOf("cloud-backup", "device-transfer").forEach { section ->
            assertThat(root.children(section).single().children("include")).isEmpty()
        }
    }

    @Test
    fun sectionsExcludeRootAndDeviceRoot() {
        listOf("cloud-backup", "device-transfer").forEach { section ->
            val excluded = root.children(section).single().children("exclude")
            assertThat(excluded.map { it.getAttribute("domain") }).containsAtLeast("root", "device_root")
            assertThat(excluded.map { it.getAttribute("path") }.distinct()).isEqualTo(listOf("."))
        }
    }
}
