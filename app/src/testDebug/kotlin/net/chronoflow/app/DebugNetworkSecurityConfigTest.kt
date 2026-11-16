package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class DebugNetworkSecurityConfigTest {
    private val root = parseXml("src/debug/res/xml/network_security_config.xml").documentElement

    @Test
    fun baseConfigForbidsCleartext() {
        assertThat(root.children("base-config").single().getAttribute("cleartextTrafficPermitted")).isEqualTo("false")
    }

    @Test
    fun onlyTheEmulatorHostAllowsCleartext() {
        val config = root.children("domain-config").single()
        assertThat(config.getAttribute("cleartextTrafficPermitted")).isEqualTo("true")
        val domain = config.children("domain").single()
        assertThat(domain.textContent.trim()).isEqualTo("10.0.2.2")
        assertThat(domain.getAttribute("includeSubdomains")).isEqualTo("false")
    }
}
