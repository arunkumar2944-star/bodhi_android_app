package com.prodigit.bodhiattendance.ui.device

import com.prodigit.bodhiattendance.data.remote.dto.RegisteredDevice

data class DeviceRegistrationUiState(

    val isLoading: Boolean = false,

    val isSuccessful: Boolean = false,

    val status: String? = null,

    val message: String? = null,

    val device: RegisteredDevice? = null,

    val error: String? = null
)