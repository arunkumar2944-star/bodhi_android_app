package com.prodigit.bodhiattendance.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.prodigit.bodhiattendance.config.AppConfig


// ============================================================
// GLOBAL APPLICATION HEADER
//
// Shared across all authenticated application screens.
//
// Branding comes from AppConfig.
// Do not hard-code branding values here.
// ============================================================

@Composable
fun AppMasterHeader(

    notificationCount: Int = 0,

    onNotificationClick: () -> Unit = {}

) {

    val appName =
        stringResource(
            id = AppConfig.appNameRes
        )

    val appSubtitle =
        stringResource(
            id = AppConfig.appSubtitleRes
        )


    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically

    ) {


        // ====================================================
        // APPLICATION LOGO
        // ====================================================

        AppLogoHeader(
            size = 52.dp
        )


        Spacer(
            modifier =
                Modifier.width(
                    10.dp
                )
        )


        // ====================================================
        // APPLICATION NAME + SUBTITLE
        // ====================================================

        Column(

            modifier =
                Modifier.weight(
                    1f
                )

        ) {

            Text(

                text =
                    appName,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary,

                maxLines =
                    1
            )


            Text(

                text =
                    appSubtitle,

                fontSize =
                    11.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                maxLines =
                    1
            )
        }


        // ====================================================
        // NOTIFICATION ICON
        // ====================================================

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
                                        notificationCount.toString()
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
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )
            }
        }
    }
}