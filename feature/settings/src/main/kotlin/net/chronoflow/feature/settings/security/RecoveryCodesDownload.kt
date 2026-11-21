package net.chronoflow.feature.settings.security

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.ui.R
import java.io.FileNotFoundException
import java.io.IOException

internal class RecoveryCodesDownload(
    private val create: (String) -> Unit,
    private val share: (Intent) -> Unit,
) {
    fun launch(
        name: String,
        codes: List<String>,
    ) {
        try {
            create(name)
        } catch (_: ActivityNotFoundException) {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            send.putExtra(Intent.EXTRA_TEXT, codes.joinToString("\n"))
            share(Intent.createChooser(send, null))
        }
    }
}

@Composable
internal fun rememberRecoveryCodesDownload(
    codes: List<String>,
    onResult: (Uri, ContentResolver) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val currentCodes by rememberUpdatedState(codes)
    val result by rememberUpdatedState(onResult)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) result(uri, resolver)
        }
    val name = stringResource(R.string.android_recovery_codes_file_name)
    return { RecoveryCodesDownload(launcher::launch, context::startActivity).launch(name, currentCodes) }
}

internal suspend fun writeRecoveryCodes(
    resolver: ContentResolver,
    uri: Uri,
    currentCodes: () -> List<String>,
) {
    val codes = currentCodes()
    if (codes.isEmpty()) {
        deleteEmptyDocument(resolver, uri)
        return
    }
    currentCoroutineContext().ensureActive()
    if (currentCodes() != codes) {
        deleteEmptyDocument(resolver, uri)
        return
    }
    val bytes = RecoveryCodesDocument.bytes(codes)
    val output = resolver.openOutputStream(uri, "wt") ?: throw IOException()
    output.use {
        currentCoroutineContext().ensureActive()
        if (currentCodes() == codes) it.write(bytes)
    }
}

private fun deleteEmptyDocument(
    resolver: ContentResolver,
    uri: Uri,
) {
    try {
        DocumentsContract.deleteDocument(resolver, uri)
    } catch (_: FileNotFoundException) {
    } catch (_: SecurityException) {
    } catch (_: IllegalArgumentException) {
    } catch (_: UnsupportedOperationException) {
    }
}
