package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context

/** Office identity only: never authorizes presence or an attendance event. */
class AttendanceOfficeContext(context: Context) {
    private val prefs = context.getSharedPreferences("attendance_office_context", Context.MODE_PRIVATE)

    private fun scope(user: Int, company: Int, device: String, day: String, boot: String) =
        "$company/$user/$device/$day/$boot"

    fun read(user: Int, company: Int, device: String, day: String, boot: String): Long? {
        if (prefs.getString("scope", null) != scope(user, company, device, day, boot)) return null
        return prefs.getLong("office", 0L).takeIf { it > 0L }
    }

    fun save(user: Int, company: Int, device: String, day: String, boot: String, office: Long) {
        require(user > 0 && company > 0 && device.isNotBlank() && day.isNotBlank() && boot.isNotBlank() && office > 0)
        check(prefs.edit().putString("scope", scope(user, company, device, day, boot))
            .putLong("office", office).commit()) { "Office context persistence failed" }
    }
}
