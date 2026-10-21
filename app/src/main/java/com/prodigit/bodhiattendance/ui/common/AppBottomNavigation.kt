package com.prodigit.bodhiattendance.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Assignment

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// COLORS
// ============================================================

private val NavigationBlue =
    Color(0xFF1267E8)

private val NavigationNavy =
    Color(0xFF142D55)

private val NavigationInactive =
    Color(0xFF6D788B)

private val SelectedBackground =
    Color(0xFFE8F2FF)


// ============================================================
// BOTTOM NAVIGATION DESTINATIONS
// ============================================================

enum class BottomNavigationDestination(

    val route: String,

    val label: String,

    val icon: ImageVector

) {

    DASHBOARD(
        route = "dashboard",
        label = "Dashboard",
        icon = Icons.Default.Dashboard
    ),

    ATTENDANCE(
        route = "attendance",
        label = "Attendance",
        icon = Icons.Default.CalendarMonth
    ),

    ON_DUTY(
        route = "on_duty",
        label = "On Duty",
        icon = Icons.Default.Work
    ),

    REQUESTS(
        route = "requests",
        label = "Requests",
        icon = Icons.Outlined.Assignment
    ),

    SETTINGS(
        route = "settings",
        label = "Settings",
        icon = Icons.Default.Settings
    )
}


// ============================================================
// APP BOTTOM NAVIGATION
//
// ON DUTY VISIBILITY RULE:
//
// showOnDuty = true
//      -> ON DUTY menu is shown.
//
// showOnDuty = false
//      -> ON DUTY menu is hidden.
//
// MainAppLayout supplies this value.
//
// Later:
//
// showOnDuty will become true only when employee has:
//
// APPROVED / ACTIVE On Duty assignment
// dated from TODAY through TODAY + 15 DAYS.
//
// ============================================================

@Composable
fun AppBottomNavigation(

    selectedDestination: BottomNavigationDestination,

    onDestinationSelected:
        (BottomNavigationDestination) -> Unit,

    // ========================================================
    // CONDITIONAL ON DUTY NAVIGATION
    // ========================================================

    showOnDuty: Boolean = false,

    modifier: Modifier = Modifier

) {


    // ========================================================
    // NAVIGATION ITEMS
    //
    // Preserve original order:
    //
    // Dashboard
    // Attendance
    // On Duty      <- conditional
    // Requests
    // Settings
    // ========================================================

    val visibleDestinations =
        BottomNavigationDestination.entries
            .filter { destination ->

                when (
                    destination
                ) {

                    BottomNavigationDestination.ON_DUTY ->

                        showOnDuty


                    else ->

                        true
                }
            }


    // ========================================================
    // NAVIGATION CONTAINER
    // ========================================================

    Box(

        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 8.dp
                )

    ) {

        Card(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(
                            28.dp
                        )
                    ),

            shape =
                RoundedCornerShape(
                    28.dp
                ),

            colors =
                CardDefaults.cardColors(

                    containerColor =
                        MaterialTheme.colorScheme.surface
                ),

            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )

        ) {


            // =================================================
            // NAVIGATION ROW
            // =================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 6.dp,
                            vertical = 8.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceEvenly,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                // =============================================
                // IMPORTANT:
                //
                // Use visibleDestinations instead of
                // BottomNavigationDestination.entries.
                //
                // This preserves all existing behavior while
                // allowing ON DUTY to disappear dynamically.
                // =============================================

                visibleDestinations.forEach { destination ->


                    val selected =
                        destination ==
                                selectedDestination


                    ProfessionalNavigationItem(

                        destination =
                            destination,

                        selected =
                            selected,


                        // =====================================
                        // IMPORTANT EXISTING FUNCTIONALITY
                        //
                        // Always forward the click.
                        //
                        // Do NOT change this behavior.
                        //
                        // This is required because System Health
                        // can visually map to Dashboard while
                        // Dashboard still needs to be clickable.
                        // =====================================

                        onClick = {

                            onDestinationSelected(
                                destination
                            )
                        },

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }
        }
    }
}


// ============================================================
// NAVIGATION ITEM
// ============================================================

@Composable
private fun ProfessionalNavigationItem(

    destination: BottomNavigationDestination,

    selected: Boolean,

    onClick: () -> Unit,

    modifier: Modifier = Modifier

) {

    Column(

        modifier =
            modifier
                .clickable(
                    onClick = onClick
                )
                .padding(
                    vertical = 3.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {


        // ====================================================
        // ICON CONTAINER
        // ====================================================

        Box(

            modifier =
                Modifier
                    .size(
                        width = 50.dp,
                        height = 38.dp
                    )
                    .background(

                        color =

                            if (
                                selected
                            ) {

                                MaterialTheme.colorScheme.primaryContainer

                            } else {

                                Color.Transparent
                            },

                        shape =
                            RoundedCornerShape(
                                18.dp
                            )
                    ),

            contentAlignment =
                Alignment.Center

        ) {


            // =================================================
            // ICON
            // =================================================

            Icon(

                imageVector =
                    destination.icon,

                contentDescription =
                    destination.label,

                modifier =
                    Modifier.size(

                        if (
                            selected
                        ) {

                            25.dp

                        } else {

                            23.dp
                        }
                    ),

                tint =

                    if (
                        selected
                    ) {

                        MaterialTheme.colorScheme.primary

                    } else {

                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
            )


            // =================================================
            // SELECTED INDICATOR
            // =================================================

            if (
                selected
            ) {

                Box(

                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomCenter
                            )
                            .padding(
                                bottom = 2.dp
                            )
                            .size(
                                4.dp
                            )
                            .background(
                                color =
                                    NavigationBlue,
                                shape =
                                    CircleShape
                            )
                )
            }
        }


        Spacer(

            modifier =
                Modifier.height(
                    3.dp
                )
        )


        // ====================================================
        // LABEL
        // ====================================================

        Text(

            text =
                destination.label,

            fontSize =
                10.sp,

            fontWeight =

                if (
                    selected
                ) {

                    FontWeight.Bold

                } else {

                    FontWeight.Medium
                },

            color =

                if (
                    selected
                ) {

                    MaterialTheme.colorScheme.primary

                } else {

                    MaterialTheme.colorScheme.onSurfaceVariant
                },

            maxLines =
                1
        )
    }
}
