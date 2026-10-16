package com.prodigit.bodhiattendance.core.monitoring

/** Drain only server-acknowledged rows; failed/partial batches remain durable for retry. */
suspend fun <T> deliverDiagnosticOutbox(
    pending: suspend () -> List<T>,
    identify: (T) -> String,
    send: suspend (List<T>) -> Set<String>,
    acknowledge: suspend (List<String>) -> Unit,
    maxBatches: Int = 10
): Boolean {
    require(maxBatches > 0)
    repeat(maxBatches) {
        val rows = pending()
        if (rows.isEmpty()) return true
        val ownIds = rows.map(identify).toSet()
        val accepted = send(rows).intersect(ownIds)
        if (accepted.isEmpty()) return false
        acknowledge(accepted.toList())
    }
    return pending().isEmpty()
}
