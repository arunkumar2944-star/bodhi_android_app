package com.prodigit.bodhiattendance.domain.attendance

sealed class TimeAnchorInitializationResult {

    data class Ready(

        val anchorUuid: String,

        val serverTime: String,

        val elapsedRealtimeMs: Long,

        val bootId: String

    ) : TimeAnchorInitializationResult()


    data class Failed(

        val reason: String,

        val throwable: Throwable? = null

    ) : TimeAnchorInitializationResult()
}