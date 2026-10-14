package com.prodigit.bodhiattendance.data.local.repository

import android.content.Context
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.entity.RegisteredDeviceEntity

/** Room is the local cache; the authenticated CRM API remains authoritative. */
class RegisteredDeviceStore(context: Context) {
    private val dao = AttendanceDatabase.getInstance(context).registeredDeviceDao()
    private val idManager = DeviceIdManager(context)

    suspend fun restoreForUser(userId: Int): String? {
        if (userId <= 0) return null
        val binding = dao.forUser(userId) ?: return null
        if (!binding.isActive || binding.deviceId.isBlank()) return null
        val localId = idManager.getDeviceId()
        if (localId != null && localId != binding.deviceId) {
            // Never silently overwrite a different installation identity.
            return null
        }
        if (localId == null) idManager.saveExistingDeviceId(binding.deviceId)
        return binding.deviceId
    }

    suspend fun saveVerified(
        userId: Int, registrationId: Int, deviceId: String,
        companyId: Int?, onboarded: Boolean
    ) {
        require(userId > 0 && registrationId > 0 && deviceId.isNotBlank())
        check(idManager.getDeviceId() == deviceId) {
            "Verified registration must match this installation UUID"
        }
        dao.upsert(RegisteredDeviceEntity(userId, registrationId, deviceId, companyId, true, onboarded))
    }
}
