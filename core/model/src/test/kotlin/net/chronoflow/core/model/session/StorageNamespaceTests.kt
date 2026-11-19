package net.chronoflow.core.model.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StorageNamespaceTests {
    @Test fun namespacesAreDistinct() {
        assertNotEquals(StorageNamespace.Api.directoryName, StorageNamespace.Mock.directoryName)
        assertEquals("net.chronoflow.app.native", StorageNamespace.Api.keychainService)
        assertEquals("net.chronoflow.app.native.mock", StorageNamespace.Mock.keychainService)
        assertEquals("net.chronoflow.app.native.session", StorageNamespace.Api.sessionKeyAlias)
        assertEquals("net.chronoflow.app.native.mock.session", StorageNamespace.Mock.sessionKeyAlias)
    }
}
