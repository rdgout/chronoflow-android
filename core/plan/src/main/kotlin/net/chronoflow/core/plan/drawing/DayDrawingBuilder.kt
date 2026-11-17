package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PlacedBlock
import net.chronoflow.core.plan.PreparedBlock
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.TimelineGeometry
import net.chronoflow.core.plan.TimelineTileFrame
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.plan.timelineBlockFrame
import net.chronoflow.core.plan.timelineLanes
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Builds a day's [DayDrawing] in the draw order of iOS `TimelineRenderer.swift` (l.192-439). Pure and safe on any
 * thread as long as each call gets its own [TextMetrics].
 */
object DayDrawingBuilder {
    /** The marker glyph is a letter the renderer draws, not copy. */
    internal const val INFO_MARKER_GLYPH = "i"
    private const val JOINER = " · "
    private const val DIVIDER_TRAVERSAL = 10000f
    private const val TICK_TRAVERSAL = 20000f

    /**
     * [precomposite] is false only for `PrecompositedAlphaTest`'s reference, which wraps each tile in a real layer.
     * In that mode every in-tile colour is the raw token at its sub-alpha and hatches use the alpha 1 table entry.
     */
    fun build(
        day: PreparedDay,
        layout: DayLayout,
        key: DayDrawingKey,
        palette: TimelinePalette,
        metrics: TextMetrics,
        precomposite: Boolean = true,
    ): DayDrawing = Builder(day, layout, key, palette, metrics, precomposite).run()

    private class Recorder {
        val ops = ByteList()
        val coords = FloatList()
        val colors = IntList()
        val args = IntList()
        val texts = ArrayList<String>()
        val textStyles = ByteList()
        val glyphs = ByteList()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var hasBounds = false

        fun bound(
            l: Float,
            t: Float,
            r: Float,
            b: Float,
        ) {
            hasBounds = true
            if (l < minX) minX = l
            if (t < minY) minY = t
            if (r > maxX) maxX = r
            if (b > maxY) maxY = b
        }

        fun clear() {
            ops.clear()
            coords.clear()
            colors.clear()
            args.clear()
            texts.clear()
            textStyles.clear()
            glyphs.clear()
            minX = Float.MAX_VALUE
            minY = Float.MAX_VALUE
            maxX = -Float.MAX_VALUE
            maxY = -Float.MAX_VALUE
            hasBounds = false
        }

        fun append(other: Recorder) {
            ops.addAll(other.ops)
            coords.addAll(other.coords)
            colors.addAll(other.colors)
            args.addAll(other.args)
            texts.addAll(other.texts)
            textStyles.addAll(other.textStyles)
            glyphs.addAll(other.glyphs)
        }

        fun rect(
            l: Float,
            t: Float,
            r: Float,
            b: Float,
            color: Int,
        ) {
            ops.add(DrawOp.RECT)
            coords.add(l)
            coords.add(t)
            coords.add(r)
            coords.add(b)
            colors.add(color)
        }

        fun line(
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            strokeW: Float,
            color: Int,
            cap: Int,
            op: Byte = DrawOp.LINE,
        ) {
            ops.add(op)
            coords.add(x0)
            coords.add(y0)
            coords.add(x1)
            coords.add(y1)
            coords.add(strokeW)
            colors.add(color)
            args.add(cap)
        }

        fun glyph(
            x: Float,
            y: Float,
            size: Float,
            glyph: TimelineGlyph,
            color: Int,
        ) {
            ops.add(DrawOp.GLYPH)
            coords.add(x)
            coords.add(y)
            coords.add(size)
            colors.add(color)
            glyphs.add(glyph.ordinal.toByte())
            bound(x, y, x + size, y + size)
        }

        fun roundRect(
            op: Byte,
            l: Float,
            t: Float,
            r: Float,
            b: Float,
            radius: Float,
            strokeW: Float,
            color: Int,
        ) {
            ops.add(op)
            coords.add(l)
            coords.add(t)
            coords.add(r)
            coords.add(b)
            coords.add(radius)
            if (op == DrawOp.RRECT_STROKE) coords.add(strokeW)
            colors.add(color)
        }
    }

    private class Builder(
        val day: PreparedDay,
        val layout: DayLayout,
        val key: DayDrawingKey,
        val palette: TimelinePalette,
        val metrics: TextMetrics,
        val precomposite: Boolean,
    ) {
        val density = key.density
        val s = key.textScale
        val geometry = TimelineGeometry(layout, key.availableWidthPx.toDouble() / density)
        val widthPx = (geometry.width * density).roundToInt()
        val heightPx = (layout.height * density).roundToInt()

        val lineH = px(PlanMetrics.laneLabelLineHeight) * s
        val titleSize = px(PlanMetrics.laneLabelSize) * s
        val tagSize = px(PlanMetrics.planPrioritySize) * s
        val tagH = px(PlanMetrics.planPriorityLineHeight) * s
        val iconSize = px(PlanMetrics.blockIcon) * s
        val pad = px(PlanMetrics.tilePadding)
        val gap = px(PlanMetrics.smallGap)
        val border = px(PlanMetrics.border)

        val main = Recorder()
        val tile = Recorder()
        val dividerFrames = FloatList()

        fun px(dp: Double): Float = (dp * density).toFloat()

        fun sizeOf(style: TimelineTextStyle): Float = if (style == TimelineTextStyle.TAG) tagSize else titleSize

        fun alpha(
            color: Int,
            a: Double,
        ): Int = TimelineColors.withAlpha(color, a)

        fun run(): DayDrawing {
            val surface2 = palette.surface2
            val laneW = px(geometry.laneWidth)
            for (i in timelineLanes.indices) {
                val x = laneX(i)
                main.rect(x, 0f, x + laneW, px(layout.height), surface2)
            }
            lines()
            tickLabels()
            tiles()
            dividers()
            markers()
            return DayDrawing(
                key,
                widthPx,
                heightPx,
                main.ops.toArray(),
                main.coords.toArray(),
                main.colors.toArray(),
                main.args.toArray(),
                main.texts.toTypedArray(),
                main.textStyles.toArray(),
                main.glyphs.toArray(),
                dividerFrames.toArray(),
                targets(),
                hitIndex(),
            )
        }

        fun hitIndex(): HitIndex {
            val tiles = geometry.tiles
            val frames = FloatArray(tiles.size * HitIndex.TILE_STRIDE)
            for ((i, tile) in tiles.withIndex()) {
                val o = i * HitIndex.TILE_STRIDE
                frames[o] = px(tile.hitFrame.minX)
                frames[o + 1] = px(tile.hitFrame.minY)
                frames[o + 2] = px(tile.hitFrame.maxX)
                frames[o + 3] = px(tile.hitFrame.maxY)
                frames[o + 4] = px(tile.frame.midY)
            }
            return HitIndex(
                Array(tiles.size) { tiles[it].blockID },
                frames,
                Array(layout.markers.size) { layout.markers[it].block.id },
                FloatArray(layout.markers.size) { px(layout.markers[it].top) },
                Array(layout.changes.size) { layout.changes[it].block.id },
                FloatArray(layout.changes.size) { px(layout.changes[it].top) },
                px(PlanMetrics.gutterWidth),
                px(PlanMetrics.minTouchTarget / 2),
            )
        }

        /** Blocks, then divider pills, then visible ticks (`DayTimelineView.swift:117-158`); "Now" belongs to the host. */
        fun targets(): List<TimelineTarget> {
            val out = ArrayList<TimelineTarget>(day.blocks.size + layout.dividers.size + layout.ticks.size)
            for ((i, block) in day.blocks.withIndex()) {
                val frame = timelineBlockFrame(block.id, geometry, layout) ?: continue
                out.add(
                    TimelineTarget(
                        TimelineTarget.Kind.BLOCK,
                        block.id,
                        block.accessibilityLabel,
                        px(frame.minX),
                        px(frame.minY),
                        px(frame.maxX),
                        px(frame.maxY),
                        i.toFloat(),
                        "trip.block.${block.id}",
                    ),
                )
            }
            for (j in layout.dividers.indices) {
                out.add(
                    TimelineTarget(
                        TimelineTarget.Kind.DIVIDER,
                        null,
                        day.dividerLabels[j],
                        dividerFrames.at(j * 4),
                        dividerFrames.at(j * 4 + 1),
                        dividerFrames.at(j * 4 + 2),
                        dividerFrames.at(j * 4 + 3),
                        DIVIDER_TRAVERSAL + j.toFloat(),
                        null,
                    ),
                )
            }
            for ((k, tick) in layout.ticks.withIndex()) {
                if (!tick.visible) continue
                val y = px(tick.top)
                out.add(
                    TimelineTarget(
                        TimelineTarget.Kind.TICK,
                        null,
                        day.ticks[k],
                        0f,
                        y - lineH / 2,
                        px(PlanMetrics.gutterWidth),
                        y + lineH / 2,
                        TICK_TRAVERSAL + k.toFloat(),
                        null,
                    ),
                )
            }
            return out
        }

        fun laneX(i: Int): Float =
            px(PlanMetrics.gutterWidth + PlanMetrics.laneGap + i * (geometry.laneWidth + PlanMetrics.laneGap))

        fun lines() {
            var visible = 0
            for (tick in layout.ticks) if (tick.visible) visible++
            if (visible == 0) return
            val rec = main
            rec.ops.add(DrawOp.LINES)
            rec.coords.add(px(PlanMetrics.line))
            val laneW = px(geometry.laneWidth)
            for (tick in layout.ticks) {
                if (!tick.visible) continue
                val y = px(tick.top)
                for (i in timelineLanes.indices) {
                    val x = laneX(i)
                    rec.coords.add(x)
                    rec.coords.add(y)
                    rec.coords.add(x + laneW)
                    rec.coords.add(y)
                }
            }
            rec.colors.add(alpha(palette.border, PlanMetrics.gridOpacity))
            rec.args.add(visible * timelineLanes.size)
        }

        fun tickLabels() {
            val width = px(PlanMetrics.gutterWidth - PlanMetrics.smallGap)
            for ((index, tick) in layout.ticks.withIndex()) {
                if (!tick.visible) continue
                text(
                    main,
                    day.ticks[index],
                    0f,
                    px(tick.top) - lineH / 2,
                    width,
                    lineH,
                    TimelineTextStyle.TICK,
                    palette.mutedForeground,
                    1f,
                    null,
                )
            }
        }

        /** The iOS `text(...)` primitive (l.138-180), minus the per-run clip: a run that cannot fit is dropped. */
        fun text(
            rec: Recorder,
            value: String,
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            style: TimelineTextStyle,
            color: Int,
            alignment: Float,
            backing: Int?,
        ) {
            if (value.isEmpty() || width <= 0f || height <= 0f || height < lineH - 0.01f) return
            val size = sizeOf(style)
            var shown = value
            var measured = metrics.measure(style, value, size)
            if (measured > width) {
                shown = metrics.ellipsize(style, value, size, width)
                if (shown.isEmpty()) return
                measured = metrics.measure(style, shown, size)
                if (measured > width) return
            }
            val x = left + max(0f, width - measured) * alignment
            val right = x + min(width, measured)
            if (backing != null) rec.rect(x, top, right, top + lineH, backing)
            rec.ops.add(DrawOp.TEXT)
            rec.coords.add(x)
            rec.coords.add(top + metrics.ascent(style, size))
            rec.coords.add(size)
            rec.colors.add(color)
            rec.texts.add(shown)
            rec.textStyles.add(style.ordinal.toByte())
            rec.bound(x, top, right, top + lineH)
        }

        fun tiles() {
            val blocks = HashMap<String, PreparedBlock>(day.blocks.size * 2)
            for (block in day.blocks) blocks.putIfAbsent(block.id, block)
            val placed = HashMap<String, PlacedBlock>()
            for (lane in timelineLanes) for (p in layout.placed[lane].orEmpty()) placed.putIfAbsent(p.block.id, p)
            for (frame in geometry.tiles) {
                val block = blocks[frame.blockID] ?: continue
                tile(frame, block, placed[frame.blockID])
            }
        }

        fun tile(
            frame: TimelineTileFrame,
            b: PreparedBlock,
            p: PlacedBlock?,
        ) {
            val type = b.block.type
            val style = TimelineCategoryStyle(type)
            val l = px(frame.frame.x)
            val t = px(frame.frame.y)
            val w = px(frame.frame.width)
            val h = px(frame.frame.height)
            val optional = b.block.priority == Priority.Optional
            val serving = type == BlockType.CaffeineServing
            val radius = if (serving) min(w, h) / 2 else px(PlanMetrics.tileRadius)
            val rTop = min(if (frame.continuesBefore) 0f else radius, min(w / 2, h / 2))
            val rBottom = min(if (frame.continuesAfter) 0f else radius, min(w / 2, h / 2))
            val groupAlpha = TimelineColors.groupAlpha(b.status, b.block.priority)

            fun col(slot: PaletteSlot): Int {
                val c = palette.color(slot)
                return if (precomposite) TimelineColors.precomposite(c, groupAlpha, palette.surface2) else c
            }

            tile.clear()
            content(frame, b, p, style, w, h, ::col)
            val needsClip = needsClip(w, h, rTop, rBottom)

            main.ops.add(DrawOp.TILE_BEGIN)
            main.coords.add(l)
            main.coords.add(t)
            main.coords.add(w)
            main.coords.add(h)
            main.coords.add(rTop)
            main.coords.add(rBottom)
            main.ops.add(DrawOp.FILL_TILE)
            main.colors.add(col(style.fill))
            if (style.pattern == TimelinePattern.DOTS) {
                main.ops.add(DrawOp.DOTS_TILE)
                main.colors.add(alpha(col(style.foreground), PlanMetrics.dotOpacity))
            } else if (style.pattern == TimelinePattern.HATCH) {
                main.ops.add(DrawOp.HATCH_TILE)
                main.args.add(TimelineColors.hatchIndex(style.foreground, if (precomposite) groupAlpha else 1.0))
            }
            if (b.block.priority == Priority.Core) {
                main.ops.add(DrawOp.CORE_BAR)
                main.coords.add(px(PlanMetrics.coreBorder))
                main.colors.add(alpha(col(PaletteSlot.Foreground), PlanMetrics.coreBorderOpacity))
            }
            outline(type, optional, style, frame, w, h, ::col)
            if (needsClip) main.ops.add(DrawOp.CLIP_TILE)
            main.append(tile)
            main.ops.add(DrawOp.TILE_END)
        }

        fun outline(
            type: BlockType,
            optional: Boolean,
            style: TimelineCategoryStyle,
            frame: TimelineTileFrame,
            w: Float,
            h: Float,
            col: (PaletteSlot) -> Int,
        ) {
            val noCaffeine = type == BlockType.NoCaffeine
            val color: Int
            val bw: Double
            when {
                optional -> {
                    color = alpha(col(PaletteSlot.Foreground), PlanMetrics.optionalBorderOpacity)
                    bw = if (noCaffeine) PlanMetrics.emphasisBorder else PlanMetrics.border
                }

                noCaffeine -> {
                    color = col(PaletteSlot.CatCaffeine)
                    bw = PlanMetrics.emphasisBorder
                }

                type == BlockType.LightOK -> {
                    color = alpha(col(PaletteSlot.CatModerateFg), PlanMetrics.tagOpacity)
                    bw = PlanMetrics.border
                }

                type == BlockType.Note -> {
                    color = col(PaletteSlot.Border)
                    bw = PlanMetrics.border
                }

                else -> {
                    return
                }
            }
            val dashKind =
                if (optional || style.dashed) {
                    if (bw == PlanMetrics.emphasisBorder) DrawOp.DASH_LONG else DrawOp.DASH_SHORT
                } else {
                    DrawOp.DASH_SOLID
                }
            val inset = px(bw) / 2
            val clampLimit = min((w - 2 * inset) / 2, (h - 2 * inset) / 2)
            val radius = if (type == BlockType.CaffeineServing) min(w, h) / 2 else px(PlanMetrics.tileRadius)
            val rTop = min(if (frame.continuesBefore) 0f else max(0f, radius - inset), clampLimit)
            val rBottom = min(if (frame.continuesAfter) 0f else max(0f, radius - inset), clampLimit)
            main.ops.add(DrawOp.OUTLINE_TILE)
            main.coords.add(inset)
            main.coords.add(px(bw))
            main.coords.add(rTop)
            main.coords.add(rBottom)
            main.colors.add(color)
            main.args.add(dashKind)
        }

        fun needsClip(
            w: Float,
            h: Float,
            rTop: Float,
            rBottom: Float,
        ): Boolean {
            if (!tile.hasBounds) return false
            if (tile.minX < 0f || tile.minY < 0f || tile.maxX > w || tile.maxY > h) return true
            return cornerCut(rTop, tile.minX, tile.minY) || cornerCut(rTop, w - tile.maxX, tile.minY) ||
                cornerCut(rBottom, tile.minX, h - tile.maxY) || cornerCut(rBottom, w - tile.maxX, h - tile.maxY)
        }

        /** The quadratic corner's inner boundary is `sqrt(x) + sqrt(y) = sqrt(r)`. */
        fun cornerCut(
            r: Float,
            a: Float,
            b: Float,
        ): Boolean = r > 0f && sqrt(a) + sqrt(b) < sqrt(r)

        fun content(
            frame: TimelineTileFrame,
            b: PreparedBlock,
            p: PlacedBlock?,
            style: TimelineCategoryStyle,
            w: Float,
            h: Float,
            col: (PaletteSlot) -> Int,
        ) {
            val type = b.block.type
            val marked = b.status != null
            val optional = b.block.priority == Priority.Optional
            val iconOnly = key.compact || frame.tiny
            val serving = type == BlockType.CaffeineServing
            val contentTop = pad + if (frame.continuesBefore) iconSize + gap else 0f
            val iconX = if (iconOnly) w / 2 - iconSize / 2 else pad
            val iconY = if (iconOnly || serving) h / 2 - iconSize / 2 else contentTop
            val fg = col(style.foreground)
            val glyph = style.glyph
            if (glyph != null) tile.glyph(iconX, iconY, iconSize, glyph, fg)
            if (type == BlockType.NoCaffeine) {
                val stroke = px(PlanMetrics.emphasisBorder)
                tile.line(iconX, iconY + iconSize, iconX + iconSize, iconY, stroke, fg, 0)
                tile.bound(
                    iconX - stroke / 2,
                    iconY - stroke / 2,
                    iconX + iconSize + stroke / 2,
                    iconY + iconSize + stroke / 2,
                )
            }
            val textColor = col(if (marked || optional) PaletteSlot.CardForeground else style.foreground)
            if (!iconOnly) {
                val backing = col(style.fill)
                val titleX = iconX + iconSize + gap
                val statusWidth = if (b.status == null) 0f else iconSize + gap
                val contentWidth = max(0f, w - 2 * pad)
                val titleWidth = max(0f, w - pad - statusWidth - titleX)
                if (serving) {
                    text(
                        tile,
                        b.title + JOINER + b.interval,
                        titleX,
                        h / 2 - lineH / 2,
                        titleWidth,
                        lineH,
                        TimelineTextStyle.TITLE,
                        textColor,
                        0.5f,
                        backing,
                    )
                } else {
                    text(
                        tile,
                        b.title,
                        titleX,
                        contentTop,
                        titleWidth,
                        lineH,
                        TimelineTextStyle.TITLE,
                        textColor,
                        0f,
                        backing,
                    )
                    val intervalTop = contentTop + lineH + gap
                    text(
                        tile,
                        b.interval,
                        pad,
                        intervalTop,
                        contentWidth,
                        min(lineH, max(0f, h - pad - intervalTop)),
                        TimelineTextStyle.TITLE,
                        textColor,
                        0f,
                        backing,
                    )
                    if (p != null && p.cols == 1 && h > px(PlanMetrics.detailsThreshold)) {
                        details(b, h, intervalTop, contentWidth, textColor, backing, col)
                    }
                }
            }
            val status = b.status
            if (status != null) {
                val glyphKind = if (status == CheckStatus.Done) TimelineGlyph.CHECK else TimelineGlyph.SKIP_FORWARD
                tile.glyph(
                    w - pad - iconSize,
                    if (iconOnly) pad else contentTop,
                    iconSize,
                    glyphKind,
                    col(PaletteSlot.Foreground),
                )
            }
            chevrons(frame, h, w, fg)
        }

        fun details(
            b: PreparedBlock,
            h: Float,
            intervalTop: Float,
            contentWidth: Float,
            textColor: Int,
            backing: Int,
            col: (PaletteSlot) -> Int,
        ) {
            val tagTop = intervalTop + lineH + gap
            val tagW = min(contentWidth, metrics.measure(TimelineTextStyle.TAG, b.priority, tagSize) + 2 * pad)
            val tagBottom = tagTop + tagH + 2 * border
            if (tagBottom > h - pad) return
            val half = border / 2
            tile.roundRect(
                DrawOp.RRECT_FILL,
                pad + half,
                tagTop + half,
                pad + tagW - half,
                tagBottom - half,
                (tagBottom - tagTop) / 2,
                0f,
                alpha(col(PaletteSlot.Background), PlanMetrics.tagOpacity),
            )
            tile.roundRect(
                DrawOp.RRECT_STROKE,
                pad + half,
                tagTop + half,
                pad + tagW - half,
                tagBottom - half,
                (tagBottom - tagTop) / 2,
                border,
                alpha(textColor, PlanMetrics.tagOpacity),
            )
            tile.bound(pad, tagTop, pad + tagW, tagBottom)
            text(
                tile,
                b.priority,
                pad + pad,
                tagTop + border,
                max(0f, tagW - 2 * pad),
                tagH,
                TimelineTextStyle.TAG,
                textColor,
                0f,
                null,
            )
            val reason = b.reason ?: return
            val y = tagBottom + gap
            text(
                tile,
                reason,
                pad,
                y,
                contentWidth,
                min(tagH, max(0f, h - pad - y)),
                TimelineTextStyle.TAG,
                textColor,
                0f,
                backing,
            )
        }

        fun chevrons(
            frame: TimelineTileFrame,
            h: Float,
            w: Float,
            color: Int,
        ) {
            val stroke = px(PlanMetrics.line)
            val mx = w / 2
            val reach = gap
            val half = stroke / 2
            if (frame.continuesBefore) {
                val y = gap
                tile.line(mx - reach, y + reach, mx, y, stroke, color, 0)
                tile.line(mx, y, mx + reach, y + reach, stroke, color, 0)
                tile.bound(mx - reach - half, y - half, mx + reach + half, y + reach + half)
            }
            if (frame.continuesAfter) {
                val y = h - gap * 2
                tile.line(mx - reach, y, mx, y + reach, stroke, color, 0)
                tile.line(mx, y + reach, mx + reach, y, stroke, color, 0)
                tile.bound(mx - reach - half, y - half, mx + reach + half, y + reach + half)
            }
        }

        fun dividers() {
            for ((index, divider) in layout.dividers.withIndex()) {
                val top = px(divider.top)
                main.line(
                    0f,
                    top,
                    px(geometry.width),
                    top,
                    px(PlanMetrics.emphasisBorder),
                    palette.catTravel,
                    DrawOp.DASH_ZONE,
                    DrawOp.DASH_LINE,
                )
                val label = day.dividerLabels[index]
                val pillH = tagH + px(PlanMetrics.zonePillVerticalPadding) * 2
                val left = px(PlanMetrics.gutterWidth + PlanMetrics.laneGap)
                val maxW = px(geometry.width - PlanMetrics.gutterWidth - PlanMetrics.laneGap)
                val pillW =
                    min(
                        maxW,
                        metrics.measure(TimelineTextStyle.TAG, label, tagSize) + px(PlanMetrics.zonePillPadding) * 2,
                    )
                val pillTop = top - pillH / 2
                main.roundRect(
                    DrawOp.RRECT_FILL,
                    left,
                    pillTop,
                    left + pillW,
                    pillTop + pillH,
                    pillH / 2,
                    0f,
                    palette.catTravel,
                )
                dividerFrames.add(left)
                dividerFrames.add(pillTop)
                dividerFrames.add(left + pillW)
                dividerFrames.add(pillTop + pillH)
                val padX = px(PlanMetrics.zonePillPadding)
                val padY = px(PlanMetrics.zonePillVerticalPadding)
                text(
                    main,
                    label,
                    left + padX,
                    pillTop + padY,
                    pillW - 2 * padX,
                    pillH - 2 * padY,
                    TimelineTextStyle.TAG,
                    palette.catTravelFg,
                    0f,
                    null,
                )
            }
        }

        fun markers() {
            val size = px(PlanMetrics.gutterMarkerSize)
            val left = px(PlanMetrics.gutterWidth - PlanMetrics.gutterMarkerSize)
            for (marker in layout.markers) {
                val top = px(marker.top) - size / 2
                main.ops.add(DrawOp.CIRCLE)
                main.coords.add(left + size / 2)
                main.coords.add(top + size / 2)
                main.coords.add(size / 2)
                main.colors.add(palette.card)
                text(
                    main,
                    INFO_MARKER_GLYPH,
                    left + gap,
                    top,
                    size - 2 * gap,
                    size,
                    TimelineTextStyle.TITLE,
                    palette.foreground,
                    0f,
                    null,
                )
            }
        }
    }
}
