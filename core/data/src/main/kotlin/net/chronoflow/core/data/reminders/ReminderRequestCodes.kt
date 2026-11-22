package net.chronoflow.core.data.reminders

object ReminderRequestCodes {
    fun allocate(
        identifier: String,
        taken: Set<Int>,
    ): Int {
        var code = identifier.hashCode() and Int.MAX_VALUE
        while (code in taken) code = (code + 1) and Int.MAX_VALUE
        return code
    }
}
