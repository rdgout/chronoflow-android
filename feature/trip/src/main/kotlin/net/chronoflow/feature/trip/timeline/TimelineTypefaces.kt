package net.chronoflow.feature.trip.timeline

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.R
import net.chronoflow.core.plan.drawing.TimelineTextStyle

/** Figtree 400, 500 and 600 as the timeline draws them; loaded once per process, never on the main thread. */
class TimelineTypefaces(
    val regular: Typeface,
    val medium: Typeface,
    val semibold: Typeface,
) {
    internal fun faceFor(style: Int): Typeface =
        when (style) {
            TimelineTextStyle.TICK.ordinal -> regular
            TimelineTextStyle.TITLE.ordinal -> semibold
            else -> medium
        }

    companion object {
        private val mutex = Mutex()

        @Volatile
        private var cached: TimelineTypefaces? = null

        suspend fun load(
            context: Context,
            io: CoroutineDispatcher = Dispatchers.IO,
        ): TimelineTypefaces {
            if (!TimelineGlyphPaths.isParsed) withContext(io) { TimelineGlyphPaths.warm() }
            cached?.let { return it }
            return mutex.withLock {
                cached ?: withContext(io) {
                    val app = context.applicationContext
                    TimelineTypefaces(
                        font(app, R.font.figtree_400),
                        font(app, R.font.figtree_500),
                        font(app, R.font.figtree_600),
                    )
                }.also { cached = it }
            }
        }

        /** Reads the loaded faces without suspending; null until [load] has finished. */
        fun cachedOrNull(): TimelineTypefaces? = cached

        private fun font(
            context: Context,
            id: Int,
        ): Typeface = checkNotNull(ResourcesCompat.getFont(context, id)) { "font $id did not load" }
    }
}
