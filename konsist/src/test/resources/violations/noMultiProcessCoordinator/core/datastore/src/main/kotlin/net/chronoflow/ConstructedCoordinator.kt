package net.chronoflow

import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

val constructedCoordinator = androidx.datastore.core.MultiProcessCoordinator(EmptyCoroutineContext, File("preferences"))
