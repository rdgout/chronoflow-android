package net.chronoflow.core.testing

fun verifyRecreatedSheet(
    requireSheet: () -> Unit,
    requireSameProcess: () -> Unit,
    dismissSheet: () -> Unit,
    requireSheetGone: () -> Unit,
    requireUnderlyingPage: () -> Unit,
) {
    requireSheet()
    requireSameProcess()
    dismissSheet()
    requireSheetGone()
    requireUnderlyingPage()
    requireSameProcess()
}
