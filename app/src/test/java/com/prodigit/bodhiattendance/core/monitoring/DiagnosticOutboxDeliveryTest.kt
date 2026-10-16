package com.prodigit.bodhiattendance.core.monitoring

import kotlin.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DiagnosticOutboxDeliveryTest {
    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return checkNotNull(result).getOrThrow()
    }

    @Test fun drainsMultipleThirtyRowBatchesBeforeReportingComplete() = runSuspend {
        val queue = (1..65).map { "id-$it" }.toMutableList()
        var sends = 0
        assertTrue(deliverDiagnosticOutbox(
            pending = { queue.take(30) }, identify = { it },
            send = { sends++; it.toSet() }, acknowledge = { queue.removeAll(it.toSet()) }
        ))
        assertEquals(3, sends)
        assertTrue(queue.isEmpty())
    }

    @Test fun httpFailureOrMissingAcknowledgementsLeavesRowsForRetry() = runSuspend {
        val queue = mutableListOf("CLOCK_MISMATCH")
        assertFalse(deliverDiagnosticOutbox(
            pending = { queue.toList() }, identify = { it },
            send = { emptySet() }, acknowledge = { fail("Unacknowledged rows must not be removed") }
        ))
        assertEquals(listOf("CLOCK_MISMATCH"), queue)
    }

    @Test fun partialAcknowledgementRemovesOnlyAcceptedRows() = runSuspend {
        val queue = mutableListOf("accepted", "pending")
        var sent = 0
        assertFalse(deliverDiagnosticOutbox(
            pending = { queue.toList() }, identify = { it },
            send = { if (sent++ == 0) setOf("accepted") else emptySet() },
            acknowledge = { queue.removeAll(it.toSet()) }
        ))
        assertEquals(listOf("pending"), queue)
    }

    @Test fun transportFailureNeverAcknowledgesRows() {
        var acknowledged = false
        try {
            runSuspend {
                deliverDiagnosticOutbox(pending = { listOf("saved") }, identify = { it },
                    send = { throw java.io.IOException("offline") }, acknowledge = { acknowledged = true })
            }
            fail("Transport failure must propagate to the retry caller")
        } catch (_: java.io.IOException) { }
        assertFalse(acknowledged)
    }

    @Test fun boundedRunReportsIncompleteWhileRowsRemain() = runSuspend {
        val queue = mutableListOf("one", "two")
        assertFalse(deliverDiagnosticOutbox(pending = { queue.take(1) }, identify = { it },
            send = { it.toSet() }, acknowledge = { queue.removeAll(it.toSet()) }, maxBatches = 1))
        assertEquals(listOf("two"), queue)
    }

    @Test fun serverCannotAcknowledgeIdsOutsideTheSentBatch() = runSuspend {
        var acknowledged = false
        assertFalse(deliverDiagnosticOutbox(pending = { listOf("ours") }, identify = { it },
            send = { setOf("someone-else") }, acknowledge = { acknowledged = true }))
        assertFalse(acknowledged)
    }

    @Test fun emptyOutboxNeedsNoNetworkRequest() = runSuspend {
        assertTrue(deliverDiagnosticOutbox<String>(pending = { emptyList() }, identify = { it },
            send = { fail("No network call for an empty queue"); emptySet() },
            acknowledge = { fail("Nothing to acknowledge") }))
    }

    @Test fun acknowledgementStorageFailureIsNotReportedAsSuccess() {
        try {
            runSuspend {
                deliverDiagnosticOutbox(pending = { listOf("saved") }, identify = { it },
                    send = { it.toSet() }, acknowledge = { throw IllegalStateException("Room unavailable") })
            }
            fail("Persistence failure must retain retry intent")
        } catch (_: IllegalStateException) { }
    }
}
