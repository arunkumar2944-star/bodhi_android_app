package com.prodigit.bodhiattendance.data.remote.dto.attendance

data class CreateTimeAnchorResponse(

    val success: Boolean,

    val message: String? = null,

    val data: TimeAnchorResponseData? = null
)

data class TimeAnchorResponseData(

    val anchorUuid: String,

    val serverTime: String,

    val elapsedRealtimeMs: Long,

    val bootId: String
)