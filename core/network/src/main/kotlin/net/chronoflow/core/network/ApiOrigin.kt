package net.chronoflow.core.network

import okhttp3.HttpUrl

internal fun ApiBaseUrl.owns(url: HttpUrl): Boolean =
    this.url.scheme == url.scheme && this.url.host == url.host && this.url.port == url.port &&
        url.encodedPath.startsWith(this.url.encodedPath.trimEnd('/') + "/")
