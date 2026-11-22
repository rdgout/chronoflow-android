package net.chronoflow.core.testing.audit

data class NativeSheetDismissStrip(
    val materialSheetWindow: Boolean,
    val nativeDismissLabel: Boolean,
    val hasTag: Boolean,
    val focusable: Boolean,
    val clickable: Boolean,
    val spansWindowWidth: Boolean,
    val insideNavigationInset: Boolean,
) {
    fun matches(): Boolean =
        materialSheetWindow && nativeDismissLabel && !hasTag && !focusable && clickable &&
            spansWindowWidth && insideNavigationInset
}
