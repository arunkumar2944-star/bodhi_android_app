package com.prodigit.bodhiattendance.data.remote.dto.attendance

data class TimeAnchorData(

    val anchorUuid: String,

    val serverTime: String,

    val elapsedRealtimeMs: Long,

    val bootId: String
)