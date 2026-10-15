package com.prodigit.bodhiattendance.ui.systemhealth

/** Read-only diagnostics. Never infer an office or change attendance policy. */
internal fun officeWifiHealth(required: Boolean?, approvedBssids: List<String>, detectedBssid: String?): HealthItemState {
    if (required == null) return HealthItemState(HealthStatus.NOT_CONFIGURED, "Office Wi-Fi", "Current office policy unavailable")
    if (!required) return HealthItemState(HealthStatus.NOT_REQUIRED, "Office Wi-Fi", "Not required by this CRM office policy")
    fun valid(value: String) = value.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
        value != "02:00:00:00:00:00" && value != "00:00:00:00:00:00"
    val approved = approvedBssids.map { it.trim() }.filter(::valid)
    if (approved.isEmpty()) return HealthItemState(HealthStatus.NOT_CONFIGURED, "Office Wi-Fi", "Wi-Fi required; CRM has no valid approved router")
    val detected = detectedBssid?.trim()?.takeIf(::valid)
        ?: return HealthItemState(HealthStatus.NOT_READY, "Office Wi-Fi", "Router identity unavailable; check Wi-Fi connection and location permission")
    return if (approved.any { it.equals(detected, ignoreCase = true) }) {
        HealthItemState(HealthStatus.READY, "Office Wi-Fi", "Connected router matches this CRM office")
    } else HealthItemState(HealthStatus.NOT_READY, "Office Wi-Fi", "Connected router does not match this CRM office")
}

internal fun officeBeaconHealth(required: Boolean?): HealthItemState = when (required) {
    null -> HealthItemState(HealthStatus.NOT_CONFIGURED, "Office BLE", "Current office policy unavailable")
    false -> HealthItemState(HealthStatus.NOT_REQUIRED, "Office BLE", "Not required by this CRM office policy")
    true -> HealthItemState(HealthStatus.NOT_READY, "Office BLE", "Required by CRM; live beacon verification is not integrated")
}
