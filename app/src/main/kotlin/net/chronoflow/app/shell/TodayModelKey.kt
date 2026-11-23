package net.chronoflow.app.shell

internal fun todayViewModelKey(epoch: Long?): String? = epoch?.let { "today:$it" }
