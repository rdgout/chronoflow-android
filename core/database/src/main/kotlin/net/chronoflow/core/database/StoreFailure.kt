package net.chronoflow.core.database

sealed class StoreFailure(
    cause: Throwable? = null,
) : Exception(null, cause) {
    data object Closed : StoreFailure()

    data object AlreadyOpen : StoreFailure()

    data object InvalidAccountID : StoreFailure()

    class Unavailable(
        cause: Throwable? = null,
    ) : StoreFailure(cause)

    class Migration(
        cause: Throwable? = null,
    ) : StoreFailure(cause)

    class StorageAttributes(
        cause: Throwable? = null,
    ) : StoreFailure(cause)
}
