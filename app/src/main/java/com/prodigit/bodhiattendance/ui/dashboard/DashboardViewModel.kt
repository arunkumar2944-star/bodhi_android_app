package com.prodigit.bodhiattendance.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prodigit.bodhiattendance.core.monitoring.AttendanceMonitorStatus
import com.prodigit.bodhiattendance.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * No fabricated check-in, break, attendance time or synchronization status.
 * Sensor readings are informational and cannot imply a confirmed check-in.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val session = SessionManager(application.applicationContext)
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            isLoading = false,
            employeeName = listOfNotNull(session.getFirstName(), session.getLastName())
                .filter { it.isNotBlank() }.joinToString(" "),
            employeeId = session.getUserId().takeIf { it > 0 }?.toString() ?: "",
            designation = session.getRole().orEmpty(),
            workStatus = AttendanceWorkStatus.UNKNOWN,
            officeName = "Location not yet verified",
            gpsReady = false,
            bleReady = false,
            syncReady = false,
            errorMessage = "Attendance status not yet loaded from the server"
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AttendanceMonitorStatus.state.collect { monitor ->
                _uiState.value = _uiState.value.copy(
                    gpsReady = monitor.running && monitor.latitude != null && monitor.longitude != null,
                    // Sensor BSSID observation is not the same as approved-office verification.
                    bleReady = false,
                    syncReady = false,
                    errorMessage = if (monitor.running)
                        "GPS/Wi-Fi observation running; office verification and attendance are pending"
                    else
                        "Background attendance monitor is not running"
                )
            }
        }
    }
}
