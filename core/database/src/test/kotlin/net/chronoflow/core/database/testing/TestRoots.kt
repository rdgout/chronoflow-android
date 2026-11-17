package net.chronoflow.core.database.testing

import java.io.File
import java.nio.file.Files

object TestRoots {
    fun temporaryStoreRoot(): File = Files.createTempDirectory("store").toFile()
}
