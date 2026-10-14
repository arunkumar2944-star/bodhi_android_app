package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import com.prodigit.bodhiattendance.ui.live.*

@Composable
fun PersonalInformationScreen(onBack: () -> Unit) {
    val live = liveUiState()
    val profile = live.profile
    PrototypePage("Personal Information", "Read-only CRM employee profile", onBack) {
        if (profile == null) DataUnavailable("Employee profile", live.errors["profile"] ?: live.errors["all"], live.loading)
        else PrototypePanel {
            PrototypeHeading(Icons.Outlined.PersonOutline, "Employee profile", "Loaded from CRM", ProtoBlue)
            PrototypeDetail("Employee name", listOfNotNull(profile.first_name, profile.last_name).joinToString(" "))
            PrototypeDetail("User ID", profile.id.toString())
            PrototypeDetail("Role", profile.role ?: "Not supplied by CRM")
            PrototypeDetail("Email", profile.email ?: "Not supplied by CRM")
            PrototypeDetail("Phone", profile.phone ?: "Not supplied by CRM")
            PrototypeDetail("Department", "Not supplied by the profile API")
        }
        PrototypeInfo("Profile editing, password changes and leave management are handled in CRM.")
    }
}
