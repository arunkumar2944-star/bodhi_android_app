package com.prodigit.bodhiattendance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prodigit.bodhiattendance.R
import com.prodigit.bodhiattendance.ui.common.AppLogoHeader


// ============================================================
// DASHBOARD HEADER
// ============================================================

@Composable
fun DashboardHeader(

    employeeName: String,

    notificationCount: Int = 0,

    onNotificationClick: () -> Unit = {}

) {

    // ========================================================
    // APP CONFIG
    //
    // Same source used by LoginScreen.
    // No hardcoded application name.
    // ========================================================

    val appName =
        stringResource(R.string.app_name)


    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        // ====================================================
        // TOP BRANDING ROW
        // ====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            // =================================================
            // LOGO + APP NAME
            // =================================================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,

                modifier =
                    Modifier.weight(1f)
            ) {

                AppLogoHeader(
                    size = 48.dp
                )


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Text(
                    text =
                        appName,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFF142D55),

                    maxLines =
                        1
                )
            }


            // =================================================
            // NOTIFICATION
            // =================================================

            IconButton(
                onClick =
                    onNotificationClick
            ) {

                BadgedBox(

                    badge = {

                        if (notificationCount > 0) {

                            Badge {

                                Text(
                                    text =
                                        if (notificationCount > 99) {

                                            "99+"

                                        } else {

                                            notificationCount
                                                .toString()
                                        }
                                )
                            }
                        }
                    }

                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Notifications,

                        contentDescription =
                            "Notifications",

                        modifier =
                            Modifier.size(27.dp),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // ====================================================
        // DASHBOARD TITLE
        // ====================================================

        Text(
            text =
                "Dashboard",

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                Color(0xFF142D55)
        )


        Spacer(
            modifier =
                Modifier.height(2.dp)
        )


        // ====================================================
        // DASHBOARD SUBTITLE
        // ====================================================

        Text(
            text =
                "Automatic Attendance • Live Status",

            fontSize =
                13.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // ====================================================
        // GREETING
        // ====================================================

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "${getGreeting()} ",

                fontSize =
                    15.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            Text(
                text =
                    employeeName.ifBlank {
                        "Employee"
                    },

                fontSize =
                    15.sp,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    Color(0xFF142D55)
            )
        }
    }
}


// ============================================================
// GREETING
//
// UI-only helper.
// Does NOT affect attendance calculation.
// ============================================================

private fun getGreeting(): String {

    val hour =
        java.util.Calendar
            .getInstance()
            .get(
                java.util.Calendar.HOUR_OF_DAY
            )


    return when (hour) {

        in 5..11 ->
            "Good morning,"

        in 12..16 ->
            "Good afternoon,"

        else ->
            "Good evening,"
    }
}