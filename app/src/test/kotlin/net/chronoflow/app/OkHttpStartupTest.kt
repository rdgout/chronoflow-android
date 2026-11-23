package net.chronoflow.app

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.network.ApiBaseUrl
import net.chronoflow.core.network.ApiClient
import net.chronoflow.core.network.ApiEndpoint
import net.chronoflow.core.testing.tiers.Logic
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.internal.OkHttpInternalApi
import okhttp3.internal.platform.Android10Platform
import okhttp3.internal.platform.Platform
import okhttp3.internal.platform.PlatformRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.net.Proxy
import java.net.UnknownHostException
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

@Logic
@OptIn(OkHttpInternalApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = android.app.Application::class)
class OkHttpStartupTest {
    @Test fun clientBuildsOnIoWithoutTheStartupInitializerOrApplicationContext() =
        runTest(timeout = 60.seconds) {
            withContext(Dispatchers.IO) {
                val provider =
                    parseXml(mergedManifest().absolutePath)
                        .documentElement
                        .children("application")
                        .single()
                        .children("provider")
                        .single { it.getAttribute("android:name") == "androidx.startup.InitializationProvider" }
                assertFalse(
                    provider.children("meta-data").any {
                        it.getAttribute("android:name") == "okhttp3.internal.platform.PlatformInitializer"
                    },
                )
            }
            val builds = AtomicInteger()
            val dnsCalls = AtomicInteger()
            val client =
                ApiClient(
                    baseUrl = ApiBaseUrl.parse("https://api.chronoflow.net/v1", release = true),
                    baseClient = {
                        assertNotSame(Looper.getMainLooper(), Looper.myLooper())
                        assertNull(PlatformRegistry.applicationContext)
                        OkHttpClient
                            .Builder()
                            .proxy(Proxy.NO_PROXY)
                            .dns {
                                dnsCalls.incrementAndGet()
                                throw UnknownHostException("artificial")
                            }.build()
                            .also {
                                assertTrue(Platform.get() is Android10Platform)
                                assertNotNull(it.sslSocketFactory)
                                assertNotNull(it.x509TrustManager)
                                assertSame(CookieJar.NO_COOKIES, it.cookieJar)
                                assertNull(PlatformRegistry.applicationContext)
                                builds.incrementAndGet()
                            }
                    },
                )
            assertEquals(0, builds.get())
            val failure =
                try {
                    client.request<Unit>(
                        ApiEndpoint(path = listOf("users", "me"), access = ApiEndpoint.Access.PublicRoute),
                    ) {
                        error("DNS must fail before a response")
                    }
                    error("expected an offline failure")
                } catch (failure: DataFailure) {
                    failure
                }
            assertSame(DataFailure.Offline, failure)
            assertEquals(1, builds.get())
            assertEquals(1, dnsCalls.get())
        }

    private fun mergedManifest(): File {
        val properties = Properties()
        requireNotNull(javaClass.classLoader?.getResourceAsStream("com/android/tools/test_config.properties")) {
            "AGP test_config.properties missing"
        }.use { properties.load(it) }
        val path = File(requireNotNull(properties.getProperty("android_merged_manifest")))
        val manifest = if (path.isAbsolute) path else File(File(storeProjectRoot(), "app"), path.path)
        assertTrue("merged manifest missing", manifest.isFile)
        return manifest
    }
}
