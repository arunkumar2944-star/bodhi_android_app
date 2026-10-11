package com.prodigit.bodhiattendance.ui.attendance

import android.app.Application
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager

import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.repository.TimeAnchorLocalRepository

import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.local.security.StoredTimeAnchor
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorManager
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorStorage

import com.prodigit.bodhiattendance.data.repository.AttendanceRepositoryImpl
import com.prodigit.bodhiattendance.domain.repository.AttendanceRepository

import kotlinx.coroutines.launch


// ============================================================
// TIME ANCHOR VIEW MODEL
//
// Development / integration ViewModel for trusted server time.
//
// Flow:
//
// Android
//    ↓
// TimeAnchorManager
//    ├── AttendanceRepository -> Express
//    └── TimeAnchorLocalRepository -> Room
//
// The lightweight TimeAnchorStorage remains the active-anchor
// cache.
//
// Room stores durable trusted-time evidence.
// ============================================================

class TimeAnchorViewModel(
    application: Application
) : AndroidViewModel(application) {


    // ============================================================
    // APPLICATION CONTEXT
    // ============================================================

    private val appContext =
        application.applicationContext


    // ============================================================
    // SESSION
    // ============================================================

    private val sessionManager =
        SessionManager(
            appContext
        )


    // ============================================================
    // DEVICE ID
    // ============================================================

    private val deviceIdManager =
        DeviceIdManager(
            appContext
        )


    // ============================================================
    // ATTENDANCE REPOSITORY
    //
    // Remote Express access.
    // ============================================================

    private val attendanceRepository:
            AttendanceRepository =
        AttendanceRepositoryImpl(
            RetrofitClient.attendanceApi
        )


    // ============================================================
    // ROOM DATABASE
    //
    // AttendanceDatabase is a singleton, so calling getInstance()
    // here does NOT create another independent database.
    // ============================================================

    private val attendanceDatabase =
        AttendanceDatabase.getInstance(
            appContext
        )


    // ============================================================
    // LOCAL TIME ANCHOR REPOSITORY
    //
    // Durable access to:
    //
    // time_anchors
    // ============================================================

    private val timeAnchorLocalRepository =
        TimeAnchorLocalRepository(
            timeAnchorDao =
                attendanceDatabase.timeAnchorDao()
        )


    // ============================================================
    // TRUSTED TIME COMPONENTS
    // ============================================================

    private val bootSessionManager =
        BootSessionManager(
            appContext
        )


    private val timeAnchorStorage =
        TimeAnchorStorage(
            appContext
        )


    // ============================================================
    // TIME ANCHOR MANAGER
    //
    // Coordinates:
    //
    // Express
    // +
    // active anchor cache
    // +
    // Room durable persistence
    // ============================================================

    private val timeAnchorManager =
        TimeAnchorManager(

            attendanceRepository =
                attendanceRepository,

            bootSessionManager =
                bootSessionManager,

            timeAnchorStorage =
                timeAnchorStorage,

            timeAnchorLocalRepository =
                timeAnchorLocalRepository
        )


    // ============================================================
    // CREATE FRESH TIME ANCHOR
    // ============================================================

    fun createTimeAnchor() {


        // --------------------------------------------------------
        // JWT
        // --------------------------------------------------------

        val token =
            sessionManager.getToken()


        if (token.isNullOrBlank()) {

            Log.e(
                "TIME_ANCHOR",
                "JWT session not available"
            )

            return
        }


        // --------------------------------------------------------
        // REGISTERED INSTALLATION DEVICE ID
        // --------------------------------------------------------

        val deviceId =
            deviceIdManager
                .getOrCreateDeviceId()


        // --------------------------------------------------------
        // CALL TRUSTED-TIME FLOW
        // --------------------------------------------------------

        viewModelScope.launch {


            Log.d(
                "TIME_ANCHOR",
                "Creating trusted time anchor"
            )


            val result =
                timeAnchorManager
                    .createFreshAnchor(

                        token =
                            token,

                        deviceId =
                            deviceId
                    )


            result.fold(

                onSuccess = { anchor ->


                    Log.d(
                        "TIME_ANCHOR",
                        "Anchor created successfully"
                    )


                    Log.d(
                        "TIME_ANCHOR",
                        "Anchor UUID: ${anchor.anchorUuid}"
                    )


                    Log.d(
                        "TIME_ANCHOR",
                        "Server Time: ${anchor.serverTime}"
                    )


                    Log.d(
                        "TIME_ANCHOR",
                        "Elapsed Realtime: ${anchor.elapsedRealtimeMs}"
                    )


                    Log.d(
                        "TIME_ANCHOR",
                        "Boot ID: ${anchor.bootId}"
                    )
                },


                onFailure = { error ->


                    Log.e(
                        "TIME_ANCHOR",
                        "Unable to create trusted time anchor",
                        error
                    )
                }
            )
        }
    }


    // ============================================================
    // GET CURRENT VALID ANCHOR
    //
    // Returns the active cached anchor after TimeAnchorManager
    // validates the current boot session.
    // ============================================================

    fun getCurrentAnchor():
            StoredTimeAnchor? {

        return timeAnchorManager
            .getCurrentAnchor()
    }


    // ============================================================
    // CLEAR ACTIVE ANCHOR
    //
    // Clears the lightweight active cache only.
    //
    // Room anchors are intentionally preserved because pending
    // offline attendance events may reference them.
    // ============================================================

    fun clearAnchor() {

        timeAnchorManager
            .clearAnchor()
    }
}