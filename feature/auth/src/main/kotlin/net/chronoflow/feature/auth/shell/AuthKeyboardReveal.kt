@file:Suppress("ktlint:compose:compositionlocal-allowlist")
@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import net.chronoflow.core.designsystem.generated.AuthTokens
import kotlin.math.max
import kotlin.math.min

internal class AuthCoordinates {
    var viewport: LayoutCoordinates? = null
    var content: LayoutCoordinates? = null
    var submit: LayoutCoordinates? = null
    val fields: MutableMap<String, LayoutCoordinates> = mutableMapOf()
    val requester: BringIntoViewRequester = BringIntoViewRequester()

    suspend fun reveal(
        focusedField: String,
        gap: Float,
    ) {
        val content = content?.takeIf { it.isAttached } ?: return
        val viewport = viewport?.takeIf { it.isAttached } ?: return
        val field = fields[focusedField]?.takeIf { it.isAttached } ?: return
        val submit = submit?.takeIf { it.isAttached } ?: return
        val f = content.localBoundingBoxOf(field, clipBounds = false)
        val s = content.localBoundingBoxOf(submit, clipBounds = false)
        val v = content.localBoundingBoxOf(viewport, clipBounds = false)
        val result = AuthLayout.reveal(f, s, Rect(v.left, v.top + gap, v.right, v.bottom - gap))
        val rect =
            if (result.fits) {
                Rect(
                    min(f.left, s.left),
                    min(f.top, s.top) - gap,
                    max(f.right, s.right),
                    max(f.bottom, s.bottom) + gap,
                )
            } else {
                Rect(f.left, f.top - gap, f.right, f.top - gap + v.height)
            }
        requester.bringIntoView(rect)
    }
}

/** Coordinates belong to the scroll host; its caller supplies the field content. */
internal val LocalAuthCoordinates =
    staticCompositionLocalOf<AuthCoordinates> { error("AuthScreen provides coordinates") }

@Composable
internal fun AuthKeyboardReveal(
    focusedField: String?,
    failureRevision: Int,
) {
    val coordinates = LocalAuthCoordinates.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val target = WindowInsets.imeAnimationTarget
    LaunchedEffect(focusedField, failureRevision) {
        if (focusedField == null) return@LaunchedEffect
        snapshotFlow { ime.getBottom(density) == target.getBottom(density) }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                withFrameNanos { }
                coordinates.reveal(focusedField, with(density) { AuthTokens.fieldGap.dp.toPx() })
            }
    }
}
