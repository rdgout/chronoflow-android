package net.chronoflow.core.plan.drawing

/**
 * Op codes of a [DayDrawing]. Every coordinate is px. Each op reads its operands from the shared forward cursors of
 * `coords`, `colors`, `args`, `texts` (with `textStyles`) and `glyphs`, in this order:
 *
 * | op | coords | colors | args | other |
 * |---|---|---|---|---|
 * | RECT | l,t,r,b | 1 | | |
 * | LINES | strokeW, then 4n | 1 | n | |
 * | TILE_BEGIN | l,t,w,h,rTop,rBottom | | | |
 * | FILL_TILE, DOTS_TILE | | 1 | | |
 * | HATCH_TILE | | | tableIndex | |
 * | CORE_BAR | barW | 1 | | |
 * | OUTLINE_TILE | inset,strokeW,rTop,rBottom | 1 | dashKind | |
 * | CLIP_TILE, TILE_END | | | | |
 * | GLYPH | x,y,size | 1 | | glyph ordinal |
 * | LINE | x0,y0,x1,y1,strokeW | 1 | cap (0 butt, 1 round) | |
 * | DASH_LINE | x0,y0,x1,y1,strokeW | 1 | dashKind | |
 * | TEXT | x,baselineY,sizePx | 1 | | text, style |
 * | RRECT_FILL | l,t,r,b,radius | 1 | | |
 * | RRECT_STROKE | l,t,r,b,radius,strokeW | 1 | | |
 * | CIRCLE | cx,cy,radius | 1 | | |
 */
object DrawOp {
    const val RECT: Byte = 1
    const val LINES: Byte = 2
    const val TILE_BEGIN: Byte = 3
    const val FILL_TILE: Byte = 4
    const val DOTS_TILE: Byte = 5
    const val HATCH_TILE: Byte = 6
    const val CORE_BAR: Byte = 7
    const val OUTLINE_TILE: Byte = 8
    const val CLIP_TILE: Byte = 9
    const val TILE_END: Byte = 10
    const val GLYPH: Byte = 11
    const val LINE: Byte = 12
    const val DASH_LINE: Byte = 13
    const val TEXT: Byte = 14
    const val RRECT_FILL: Byte = 15
    const val RRECT_STROKE: Byte = 16
    const val CIRCLE: Byte = 17

    /** Dash kinds: 0 solid, 1 `[3, 3]` dp, 2 `[6, 6]` dp, 3 `[2, 4]` dp with round caps. */
    const val DASH_SOLID = 0
    const val DASH_SHORT = 1
    const val DASH_LONG = 2
    const val DASH_ZONE = 3

    fun name(op: Byte): String =
        when (op) {
            RECT -> "RECT"
            LINES -> "LINES"
            TILE_BEGIN -> "TILE_BEGIN"
            FILL_TILE -> "FILL_TILE"
            DOTS_TILE -> "DOTS_TILE"
            HATCH_TILE -> "HATCH_TILE"
            CORE_BAR -> "CORE_BAR"
            OUTLINE_TILE -> "OUTLINE_TILE"
            CLIP_TILE -> "CLIP_TILE"
            TILE_END -> "TILE_END"
            GLYPH -> "GLYPH"
            LINE -> "LINE"
            DASH_LINE -> "DASH_LINE"
            TEXT -> "TEXT"
            RRECT_FILL -> "RRECT_FILL"
            RRECT_STROKE -> "RRECT_STROKE"
            CIRCLE -> "CIRCLE"
            else -> "OP$op"
        }
}
