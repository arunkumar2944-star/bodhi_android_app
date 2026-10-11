package com.prodigit.bodhiattendance.data.remote.dto.attendance

data class TimeAnchorRequest(

    val deviceId: String,

    val elapsedRealtimeMs: Long,

    val bootId: String
)