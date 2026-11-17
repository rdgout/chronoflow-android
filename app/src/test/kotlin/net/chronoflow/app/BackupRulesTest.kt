package net.chronoflow.app

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

internal fun storeProjectRoot(): File {
    var dir: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
    while (dir != null) {
        if (File(dir, "gradlew").isFile && File(dir, "settings.gradle.kts").isFile) return dir
        dir = dir.parentFile
    }
    error("project root not found")
}

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackupRulesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun allowBackupIsOff() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test fun mergedManifestPointsAtTheRules() {
        val properties = Properties()
        requireNotNull(javaClass.classLoader?.getResourceAsStream("com/android/tools/test_config.properties")) {
            "AGP test_config.properties missing"
        }.use { properties.load(it) }
        val path = File(requireNotNull(properties.getProperty("android_merged_manifest")))
        val manifest = if (path.isAbsolute) path else File(File(storeProjectRoot(), "app"), path.path)
        assertTrue("merged manifest missing", manifest.isFile)
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val application =
            factory
                .newDocumentBuilder()
                .parse(
                    manifest,
                ).getElementsByTagName("application")
                .item(0) as org.w3c.dom.Element
        val namespace = "http://schemas.android.com/apk/res/android"
        assertEquals("false", application.getAttributeNS(namespace, "allowBackup"))
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS(namespace, "dataExtractionRules"))
    }

    @Test fun dataExtractionRulesExcludeEveryDomain() {
        val domains =
            setOf(
                "root",
                "file",
                "database",
                "sharedpref",
                "external",
                "device_root",
                "device_file",
                "device_database",
                "device_sharedpref",
            )
        val excluded = mutableMapOf<String, MutableSet<String>>()
        var section: String? = null
        context.resources.getXml(R.xml.data_extraction_rules).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer" -> {
                            section = parser.name
                            excluded[parser.name] = mutableSetOf()
                        }

                        "include" -> {
                            fail("backup include present")
                        }

                        "exclude" -> {
                            assertEquals(".", parser.getAttributeValue(null, "path"))
                            excluded.getValue(requireNotNull(section)).add(parser.getAttributeValue(null, "domain"))
                        }
                    }
                }
                parser.next()
            }
        }
        assertEquals(setOf("cloud-backup", "device-transfer"), excluded.keys)
        assertEquals(domains, excluded.getValue("cloud-backup"))
        assertEquals(domains, excluded.getValue("device-transfer"))
    }
}
