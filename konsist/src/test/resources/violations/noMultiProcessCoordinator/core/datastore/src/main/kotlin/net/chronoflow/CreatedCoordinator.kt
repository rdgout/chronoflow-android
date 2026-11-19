package net.chronoflow

import androidx.datastore.core.createMultiProcessCoordinator as coordinator
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

val createdCoordinator = coordinator(EmptyCoroutineContext, File("preferences"))
