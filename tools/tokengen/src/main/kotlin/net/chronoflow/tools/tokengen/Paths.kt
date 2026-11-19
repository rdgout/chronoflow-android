package net.chronoflow.tools.tokengen

import java.nio.file.Path

fun Path.design(file: String): String = resolve("_design/canvas/$file").readText()

fun Path.ios(file: String): String = resolve("tools/tokengen/ios/$file").readText()
