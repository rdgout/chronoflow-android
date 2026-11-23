package net.chronoflow.core.navigation

fun Route.hidesShellChrome(): Boolean = this is Route.Walkthrough
