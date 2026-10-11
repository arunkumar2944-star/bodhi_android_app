package com.prodigit.bodhiattendance.data.remote.dto.attendance

data class TimeAnchorResponse(

    val success: Boolean,

    val message: String,

    val data: TimeAnchorData?
)