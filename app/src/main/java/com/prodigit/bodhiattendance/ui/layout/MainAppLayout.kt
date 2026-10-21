
package com.prodigit.bodhiattendance.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.prodigit.bodhiattendance.ui.common.AppBackground
import com.prodigit.bodhiattendance.ui.common.AppBottomNavigation
import com.prodigit.bodhiattendance.ui.common.AppMasterHeader
import com.prodigit.bodhiattendance.ui.common.BottomNavigationDestination

// ============================================================
// MAIN APPLICATION MASTER LAYOUT
//
// Owns:
// - Global background
// - Global header
// - Current page body
// - Global bottom navigation
//
// Scrollable content is managed by individual screens.
// This prevents nested-scroll and LazyColumn conflicts.
// ============================================================

@Composable
fun MainAppLayout(

    selectedDestination: BottomNavigationDestination,

    onDestinationSelected: (
        BottomNavigationDestination
    ) -> Unit,

    notificationCount: Int = 0,

    onNotificationClick: () -> Unit = {},

    // ON DUTY visibility is provided by ViewModel/Repository.
    hasUpcomingOnDuty: Boolean = false,

    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    content: @Composable () -> Unit

) {

    AppBackground {

        Scaffold(
            modifier = Modifier.fillMaxSize(),

            containerColor = Color.Transparent,

            // =================================================
            // BOTTOM NAVIGATION
            // =================================================

            bottomBar = {

                AppBottomNavigation(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected,
                    showOnDuty = hasUpcomingOnDuty
                )
            }

        ) { innerPadding ->

            // =================================================
            // MASTER CONTENT AREA
            // =================================================

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                // =============================================
                // GLOBAL HEADER - FIXED
                // =============================================

                AppMasterHeader(
                    notificationCount = notificationCount,
                    onNotificationClick = onNotificationClick
                )

                // =============================================
                // SCREEN CONTENT AREA
                //
                // Only this section changes during navigation.
                // Each screen owns its scrolling.
                // =============================================

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {

                    content()
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRefresh, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp).size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Refreshing...")
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Text(" Refresh")
                        }
                    }
                }
            }
        }
    }
}
