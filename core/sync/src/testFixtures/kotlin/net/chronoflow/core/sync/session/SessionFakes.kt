package net.chronoflow.core.sync.session

import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.identity
import net.chronoflow.core.testing.sync.syncAccount
import java.io.IOException

class SessionEventLog {
    val events = mutableListOf<String>()

    fun record(event: String) {
        events += event
    }
}

class MemorySessionStore(
    var data: ByteArray? = null,
    val log: SessionEventLog = SessionEventLog(),
    var writeGate: TestGate? = null,
) : SessionStore {
    var reads = 0
    var deletes = 0
    var readFails = false
    var writeFails = false
    var deleteFails = false

    override suspend fun read(): ByteArray? {
        reads++
        log.record("read")
        if (readFails) throw IOException()
        return data
    }

    override suspend fun write(data: ByteArray) {
        log.record("write")
        val gate = writeGate
        writeGate = null
        gate?.block()
        if (writeFails) throw IOException()
        this.data = data.copyOf()
    }

    override suspend fun delete() {
        if (deleteFails) throw IOException()
        log.record("delete")
        data = null
        deletes++
    }

    override fun toString() = "MemorySessionStore(<redacted>)"
}

class RecordingAccountStore(
    val log: SessionEventLog = SessionEventLog(),
    var gate: TestGate? = null,
) : SessionAccountStore {
    var openFails = false
    var upsertFails = false
    var accountID: AccountID? = null

    override suspend fun open(id: AccountID) {
        log.record("open:${id.rawValue}")
        val waiting = gate
        gate = null
        waiting?.block()
        if (openFails) throw IOException()
        accountID = id
    }

    override suspend fun upsert(account: SessionAccount) {
        log.record("upsert:${account.id}")
        if (upsertFails) throw IOException()
    }

    override suspend fun close() {
        accountID = null
        log.record("close")
    }
}

fun sessionResult(id: String = syncAccount): SessionResult = SessionResult("test-token", identity(id))
