package net.chronoflow.buildlogic.manifest

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

enum class Rule {
    PERMISSION,
    CUSTOM_SCHEME,
    EXPORTED_COMPONENT,
    DEBUGGABLE_OR_TEST_ONLY,
    CLEARTEXT,
    MISSING_LOCALE_CONFIG,
    BACKUP,
    DIRECT_BOOT,
    STARTUP_INITIALIZER,
    APP_LINK,
}

data class ManifestViolation(
    val rule: Rule,
    val detail: String,
) {
    override fun toString() = "$rule: $detail"
}

object ManifestRules {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private const val APP_NAMESPACE = "net.chronoflow.app"
    private const val MAIN_ACTIVITY = "net.chronoflow.app.MainActivity"
    private const val DUMP = "android.permission.DUMP"
    private val startupInitializers =
        setOf(
            "androidx.lifecycle.ProcessLifecycleInitializer",
            "androidx.profileinstaller.ProfileInstallerInitializer",
        )
    private val componentTags = setOf("activity", "activity-alias", "service", "receiver", "provider")
    private val basePermissions =
        setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.WAKE_LOCK",
        )

    fun check(
        manifestXml: String,
        applicationId: String,
        allowedExportedComponents: Set<String>,
    ): List<ManifestViolation> {
        val document = parse(manifestXml)
        val violations = mutableListOf<ManifestViolation>()
        val allowedPermissions = basePermissions + "$applicationId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

        val manifest = document.documentElement
        manifest
            .childElements("uses-permission")
            .plus(manifest.childElements("uses-permission-sdk-23"))
            .map { it.android("name") }
            .filter { it !in allowedPermissions }
            .forEach { violations += ManifestViolation(Rule.PERMISSION, it) }

        val application = manifest.childElements("application").firstOrNull()
        if (application != null) {
            if (application.android("debuggable") == "true") {
                violations += ManifestViolation(Rule.DEBUGGABLE_OR_TEST_ONLY, "android:debuggable=\"true\"")
            }
            if (application.android("testOnly") == "true") {
                violations += ManifestViolation(Rule.DEBUGGABLE_OR_TEST_ONLY, "android:testOnly=\"true\"")
            }
            if (application.android("usesCleartextTraffic") != "false") {
                violations += ManifestViolation(Rule.CLEARTEXT, "android:usesCleartextTraffic is not \"false\"")
            }
            if (application.hasAndroid("networkSecurityConfig")) {
                violations += ManifestViolation(Rule.CLEARTEXT, "android:networkSecurityConfig is present")
            }
            if (!application.hasAndroid("localeConfig")) {
                violations += ManifestViolation(Rule.MISSING_LOCALE_CONFIG, "<application> has no android:localeConfig")
            }
            if (application.android("allowBackup") != "false") {
                violations += ManifestViolation(Rule.BACKUP, "android:allowBackup is not \"false\"")
            }
            application
                .childElements("provider")
                .filter { it.android("name") == "androidx.startup.InitializationProvider" }
                .flatMap { it.childElements("meta-data") }
                .filter { it.android("value") == "androidx.startup" && it.android("name") !in startupInitializers }
                .forEach { violations += ManifestViolation(Rule.STARTUP_INITIALIZER, it.android("name")) }
            application.childElements().filter { it.tagName in componentTags }.forEach { component ->
                val name = qualify(component.android("name"))
                val exported = component.android("exported") == "true"
                val guarded = component.android("permission") == DUMP
                if (exported && name != MAIN_ACTIVITY && !guarded && name !in allowedExportedComponents) {
                    violations += ManifestViolation(Rule.EXPORTED_COMPONENT, name)
                }
            }
        }

        document
            .getElementsByTagName("data")
            .asElements()
            .map { it.android("scheme") }
            .filter { it.isNotEmpty() && it != "https" }
            .forEach { violations += ManifestViolation(Rule.CUSTOM_SCHEME, it) }

        document
            .getElementsByTagName("*")
            .asElements()
            .filter { it.android("directBootAware") == "true" }
            .forEach { violations += ManifestViolation(Rule.DIRECT_BOOT, "${it.tagName} ${it.android("name")}".trim()) }

        val verified =
            document
                .getElementsByTagName("intent-filter")
                .asElements()
                .filter { it.android("autoVerify") == "true" }
        val main =
            application?.childElements("activity")?.singleOrNull {
                qualify(it.android("name")) == MAIN_ACTIVITY
            }
        val filter = verified.singleOrNull()
        if (filter == null || filter.parentNode != main || !validAuthLinkFilter(filter)) {
            violations +=
                ManifestViolation(Rule.APP_LINK, "expected one verified HTTPS auth link filter on MainActivity")
        }
        return violations
    }

    private fun validAuthLinkFilter(filter: Element): Boolean {
        val data = filter.childElements("data")
        val actions = filter.childElements("action").map { it.android("name") }.toSet()
        val categories = filter.childElements("category").map { it.android("name") }.toSet()

        fun values(name: String): Set<String> = data.map { it.android(name) }.filter { it.isNotEmpty() }.toSet()
        return actions == setOf("android.intent.action.VIEW") &&
            categories == setOf("android.intent.category.DEFAULT", "android.intent.category.BROWSABLE") &&
            values("scheme") == setOf("https") && values("host") == setOf("app.chronoflow.net") &&
            values("pathPrefix") == setOf("/reset-password/", "/email/verify/") &&
            data.all { element ->
                (0 until element.attributes.length).all { index ->
                    val attr = element.attributes.item(index)
                    attr.namespaceURI == ANDROID_NS && attr.localName in setOf("scheme", "host", "pathPrefix")
                }
            }
    }

    private fun qualify(name: String) = if (name.startsWith(".")) APP_NAMESPACE + name else name

    private fun parse(xml: String): Document {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
    }

    private fun Element.android(name: String): String = getAttributeNS(ANDROID_NS, name)

    private fun Element.hasAndroid(name: String): Boolean = hasAttributeNS(ANDROID_NS, name)

    private fun Element.childElements(tag: String? = null): List<Element> =
        (0 until childNodes.length)
            .map { childNodes.item(it) }
            .filterIsInstance<Element>()
            .filter { tag == null || it.tagName == tag }

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        (0 until length).map { item(it) }.filterIsInstance<Element>()
}
