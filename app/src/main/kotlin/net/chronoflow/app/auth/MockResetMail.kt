package net.chronoflow.app.auth

import net.chronoflow.app.MockSources
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.network.DataSourceFactory

internal fun mockResetMail(
    selection: DataSourceFactory.Source,
    sources: MockSources?,
    publicSource: suspend () -> DataSource,
    open: (ResetPasswordLink) -> Unit,
): (suspend (String) -> Boolean)? {
    if (selection != DataSourceFactory.Source.Mock || sources == null) return null
    return mail@{ email ->
        val link = sources.latestResetLink(publicSource(), email) ?: return@mail false
        open(link)
        true
    }
}
