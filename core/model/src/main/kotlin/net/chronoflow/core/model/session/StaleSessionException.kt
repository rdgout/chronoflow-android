package net.chronoflow.core.model.session

import kotlinx.coroutines.CancellationException

class StaleSessionException : CancellationException("stale session")
