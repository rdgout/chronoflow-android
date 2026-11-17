package net.chronoflow.feature.trip.timeline

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a drawing build and reports its failures to [onFailure]. A cancellation is rethrown first: it is an
 * [IllegalStateException] subclass, so the specific catches below would otherwise swallow routine cancellation.
 */
internal inline fun recoverDrawingFailure(
    onFailure: (Exception) -> Unit,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IllegalStateException) {
        onFailure(e)
    } catch (e: IllegalArgumentException) {
        onFailure(e)
    } catch (e: IndexOutOfBoundsException) {
        onFailure(e)
    } catch (e: NoSuchElementException) {
        onFailure(e)
    }
}
