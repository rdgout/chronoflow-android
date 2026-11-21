package net.chronoflow.feature.settings.security

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecoveryCodesDownloadTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val codes = listOf("8f3k-29dm", "q7zp-4wxr")

    @Test fun downloadCreatesATextDocument() {
        var intent: Intent? = null
        RecoveryCodesDownload(
            { intent = ActivityResultContracts.CreateDocument("text/plain").createIntent(context, it) },
            {},
        ).launch(context.getString(R.string.android_recovery_codes_file_name), codes)
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent?.action)
        assertEquals("text/plain", intent?.type)
        assertEquals("Recovery codes.txt", intent?.getStringExtra(Intent.EXTRA_TITLE))
    }

    @Test fun dutchFileName() {
        val config =
            android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("nl"))
            }
        val dutch = context.createConfigurationContext(config)
        assertEquals("Herstelcodes.txt", dutch.getString(R.string.android_recovery_codes_file_name))
    }

    @Test fun noHandlerFallsBackToTheShareChooser() {
        var chooser: Intent? = null
        RecoveryCodesDownload(
            { throw ActivityNotFoundException() },
            { chooser = it },
        ).launch("Recovery codes.txt", codes)
        assertEquals(Intent.ACTION_CHOOSER, chooser?.action)
        val send = IntentCompat.getParcelableExtra(checkNotNull(chooser), Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals(Intent.ACTION_SEND, send?.action)
        assertEquals("text/plain", send?.type)
        assertEquals(codes.joinToString("\n"), send?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test fun resultUriReceivesExactlyTheDocumentBytes() =
        runTest(timeout = 60.seconds) {
            val file = File.createTempFile("recovery-test", ".txt")
            try {
                val uri =
                    checkNotNull(
                        ActivityResultContracts.CreateDocument("text/plain").parseResult(
                            Activity.RESULT_OK,
                            Intent().setData(Uri.fromFile(file)),
                        ),
                    )
                file.writeText("old content which is longer than the new recovery document by a long way")
                writeRecoveryCodes(context.contentResolver, uri) { codes }
                assertArrayEquals(RecoveryCodesDocument.bytes(codes), file.readBytes())
            } finally {
                file.delete()
            }
        }

    @Test fun resultAfterClearSecretsWritesNothing() =
        runTest(timeout = 60.seconds) {
            val file = File.createTempFile("recovery-test", ".txt").apply { delete() }
            val model =
                SettingsSecurityModel(
                    {
                        net.chronoflow.core.model.api.AccountSecurityResult.Codes(
                            net.chronoflow.core.model.api
                                .ApiRecoveryCodes(codes),
                        )
                    },
                    { java.time.Instant.parse("2026-11-21T12:00:00Z") },
                )
            model.showPrompt(SettingsSecurityPrompt.Reveal)
            model.submitPrompt(
                net.chronoflow.feature.settings.testing
                    .FakeSecretField("password"),
            )
            model.clearSecrets()
            writeRecoveryCodes(context.contentResolver, Uri.fromFile(file)) { model.state.value.codes }
            assertFalse(file.exists())
        }
}
