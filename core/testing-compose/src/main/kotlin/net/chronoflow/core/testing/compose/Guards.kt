package net.chronoflow.core.testing.compose

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.ui.semantics.Ellipsizable
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reference-free layout guards. Each throws an [AssertionError] naming the node and both rectangles.
 *
 * Lazy lists expose only composed items, so the guards see composed nodes only. Text in a scrolling container
 * is checked after `performScrollTo()`. The guards enumerate nodes with specific matchers; screen tests never do.
 */
object Guards {
    private const val TOLERANCE = 0.5f
    private const val MIN_OVERLAP_DP = 48f

    private val interactive =
        SemanticsMatcher("interactive") {
            SemanticsActions.OnClick in it.config ||
                SemanticsProperties.ToggleableState in it.config ||
                SemanticsActions.SetText in it.config
        }
    private val hasText = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)

    private fun describe(node: SemanticsNode): String {
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        return tag?.let { "tag '$it'" } ?: text?.let { "text '$it'" } ?: "node ${node.id}"
    }

    private fun rect(r: Rect) = "[${r.left}, ${r.top}, ${r.right}, ${r.bottom}]"

    private fun ancestors(node: SemanticsNode): Sequence<SemanticsNode> = generateSequence(node.parent) { it.parent }

    private fun hasScrollAncestor(node: SemanticsNode) =
        ancestors(node).any {
            SemanticsProperties.VerticalScrollAxisRange in it.config ||
                SemanticsProperties.HorizontalScrollAxisRange in it.config
        }

    private fun byId(id: Int) = SemanticsMatcher("id=$id") { it.id == id }

    /**
     * Scrolls a node into view when a scroll container holds it, then returns its fresh bounds. `performScrollTo`
     * aligns the node's bottom with the container's bottom, which a full-window scroller leaves under the system
     * bars, so with a [safe] area the scroll continues until the node lies inside the viewport and the safe area.
     * A node taller than that region is returned clipped to it when at least [minOverlap] px of it stay visible.
     */
    private fun settledBounds(
        rule: SemanticsNodeInteractionsProvider,
        node: SemanticsNode,
        useUnmergedTree: Boolean,
        safe: Rect? = null,
        minOverlap: Float = 0f,
    ): Rect {
        if (!hasScrollAncestor(node)) return node.boundsInRoot
        val interaction = rule.onNode(byId(node.id), useUnmergedTree)
        interaction.performScrollTo()
        var bounds = interaction.fetchSemanticsNode().boundsInRoot
        val scroller =
            ancestors(node).firstOrNull { SemanticsProperties.VerticalScrollAxisRange in it.config }
        if (safe == null || scroller == null) return bounds
        val region = scroller.boundsInRoot.intersect(safe)
        val fits = bounds.height <= region.height + TOLERANCE
        val dy =
            when {
                !fits -> bounds.top - region.top
                bounds.bottom > region.bottom + TOLERANCE -> bounds.bottom - region.bottom
                bounds.top < region.top - TOLERANCE -> bounds.top - region.top
                else -> 0f
            }
        if (abs(dy) > TOLERANCE) {
            rule
                .onNode(byId(scroller.id), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, dy) }
            bounds = rule.onNode(byId(node.id), useUnmergedTree).fetchSemanticsNode().boundsInRoot
        }
        if (fits) return bounds
        val visible = minOf(bounds.bottom, region.bottom) - maxOf(bounds.top, region.top)
        return if (visible >=
            minOverlap
        ) {
            Rect(bounds.left, maxOf(bounds.top, region.top), bounds.right, minOf(bounds.bottom, region.bottom))
        } else {
            bounds
        }
    }

    private fun windowPx(profile: DeviceProfile) =
        profile.widthDp * profile.density to profile.heightDp * profile.density

    private fun isExempt(
        node: SemanticsNode,
        exemptTags: Set<String>,
    ): Boolean =
        exemptTags.isNotEmpty() &&
            (sequenceOf(node) + ancestors(node)).any { it.config.getOrNull(SemanticsProperties.TestTag) in exemptTags }

    fun assertTextFits(
        rule: SemanticsNodeInteractionsProvider,
        profile: DeviceProfile,
    ) {
        val (width, height) = windowPx(profile)
        for (node in rule.onAllNodes(hasText, useUnmergedTree = true).fetchSemanticsNodes()) {
            val bounds = settledBounds(rule, node, useUnmergedTree = true)
            val horizontal = bounds.left >= -TOLERANCE && bounds.right <= width + TOLERANCE
            val vertical = bounds.top >= -TOLERANCE && bounds.bottom <= height + TOLERANCE
            if (!horizontal || !vertical) {
                throw AssertionError("${describe(node)} leaves the window: ${rect(bounds)} vs [0, 0, $width, $height]")
            }
            val allowed = (sequenceOf(node) + ancestors(node)).any { Ellipsizable in it.config }
            if (allowed) continue
            val results = mutableListOf<TextLayoutResult>()
            node.config
                .getOrNull(SemanticsActions.GetTextLayoutResult)
                ?.action
                ?.invoke(results)
            for (layout in results) {
                val ellipsized = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
                // hasVisualOverflow is true for any text narrower than its paragraph, so measure the lines instead.
                val lastLine = layout.lineCount - 1
                val clipped =
                    layout.multiParagraph.didExceedMaxLines ||
                        (0..lastLine).any {
                            layout.getLineRight(it) - layout.getLineLeft(it) >
                                layout.size.width + TOLERANCE
                        } ||
                        layout.getLineBottom(lastLine) > layout.size.height + TOLERANCE
                if (ellipsized || clipped) {
                    throw AssertionError("${describe(node)} is truncated: ${rect(bounds)}, ellipsized=$ellipsized")
                }
            }
        }
    }

    fun assertTouchTargets(
        rule: SemanticsNodeInteractionsProvider,
        profile: DeviceProfile,
        minSize: Dp = 48.dp,
        exemptTags: Set<String> = emptySet(),
    ) {
        val min = minSize.value * profile.density
        for (node in rule.onAllNodes(interactive).fetchSemanticsNodes()) {
            if (isExempt(node, exemptTags)) continue
            // Layout size, never touchBoundsInRoot: Compose enlarges touch bounds, so a 40 dp box would pass.
            // layoutInfo spans the whole modifier chain, so Material's minimumInteractiveComponentSize counts.
            val width = node.layoutInfo.width
            val height = node.layoutInfo.height
            if (width < min - TOLERANCE || height < min - TOLERANCE) {
                throw AssertionError("${describe(node)} is $width x $height px, below $min px")
            }
        }
    }

    fun assertInteractiveInSafeArea(
        rule: SemanticsNodeInteractionsProvider,
        profile: DeviceProfile,
        exemptTags: Set<String> = emptySet(),
    ) {
        val (width, height) = windowPx(profile)
        val d = profile.density
        val safe =
            Rect(
                profile.insets.left * d,
                profile.insets.top * d,
                width - profile.insets.right * d,
                height - profile.insets.bottom * d,
            )
        for (node in rule.onAllNodes(interactive).fetchSemanticsNodes()) {
            if (isExempt(node, exemptTags)) continue
            val bounds =
                settledBounds(rule, node, useUnmergedTree = false, safe = safe, minOverlap = MIN_OVERLAP_DP * d)
            val inside =
                bounds.left >= safe.left - TOLERANCE && bounds.top >= safe.top - TOLERANCE &&
                    bounds.right <= safe.right + TOLERANCE && bounds.bottom <= safe.bottom + TOLERANCE
            if (!inside) {
                throw AssertionError(
                    "${describe(node)} ${rect(bounds)} lies outside the safe area ${rect(safe)}",
                )
            }
        }
    }

    fun assertNoInteractiveOverlap(
        rule: SemanticsNodeInteractionsProvider,
        exemptTags: Set<String> = emptySet(),
    ) {
        val nodes = rule.onAllNodes(interactive).fetchSemanticsNodes().filterNot { isExempt(it, exemptTags) }
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val a = nodes[i]
                val b = nodes[j]
                if (ancestors(a).any { it.id == b.id } || ancestors(b).any { it.id == a.id }) continue
                val x =
                    minOf(a.boundsInRoot.right, b.boundsInRoot.right) - maxOf(a.boundsInRoot.left, b.boundsInRoot.left)
                val y =
                    minOf(a.boundsInRoot.bottom, b.boundsInRoot.bottom) - maxOf(a.boundsInRoot.top, b.boundsInRoot.top)
                if (x > TOLERANCE && y > TOLERANCE) {
                    throw AssertionError(
                        "${describe(a)} ${rect(a.boundsInRoot)} overlaps ${describe(b)} ${rect(b.boundsInRoot)}",
                    )
                }
            }
        }
    }

    fun assertHeadings(
        rule: SemanticsNodeInteractionsProvider,
        vararg headingTags: String,
    ) {
        require(headingTags.isNotEmpty()) { "assertHeadings needs at least one tag" }
        for (tag in headingTags) {
            val nodes = rule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()
            if (nodes.size != 1) throw AssertionError("tag '$tag' exists ${nodes.size} times, expected exactly once")
            if (SemanticsProperties.Heading !in
                nodes.single().config
            ) {
                throw AssertionError("tag '$tag' is not a heading")
            }
        }
    }

    fun assertInk(
        rule: SemanticsNodeInteractionsProvider,
        profile: DeviceProfile,
        minInkFraction: Float = 0.005f,
    ) {
        val image = rule.onRoot().captureToImage()
        val (width, height) = windowPx(profile)
        if (abs(image.width - width.roundToInt()) > 1 || abs(image.height - height.roundToInt()) > 1) {
            throw AssertionError(
                "capture is ${image.width}x${image.height} px, expected ${width.roundToInt()}x${height.roundToInt()}",
            )
        }
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels, 0, 0, image.width, image.height)
        val counts = HashMap<Int, Int>()
        for (p in pixels) counts.merge(p, 1, Int::plus)
        val background = counts.maxBy { it.value }.key
        var ink = 0
        for (p in pixels) if (differs(p, background)) ink++
        val fraction = ink.toFloat() / pixels.size
        if (fraction <
            minInkFraction
        ) {
            throw AssertionError("ink fraction $fraction is below $minInkFraction: the screen is blank")
        }
    }

    private fun differs(
        a: Int,
        b: Int,
    ): Boolean =
        abs((a ushr 24) - (b ushr 24)) > 16 ||
            abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) > 16 ||
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) > 16 ||
            abs((a and 0xFF) - (b and 0xFF)) > 16

    fun assertAll(
        rule: SemanticsNodeInteractionsProvider,
        profile: DeviceProfile,
        headingTags: Set<String>,
        exemptTags: Set<String> = emptySet(),
    ) {
        assertTextFits(rule, profile)
        assertTouchTargets(rule, profile, exemptTags = exemptTags)
        assertInteractiveInSafeArea(rule, profile, exemptTags)
        assertNoInteractiveOverlap(rule, exemptTags)
        assertHeadings(rule, *headingTags.toTypedArray())
        assertInk(rule, profile)
    }
}
