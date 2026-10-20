package com.prodigit.bodhiattendance.core.session

import android.content.Context

class SessionManager(
    context: Context
) {
    private val preferencesContext = context.applicationContext

    private val preferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )


    // ============================================================
    // SAVE LOGIN SESSION
    // ============================================================

    fun saveSession(
        token: String,
        userId: Int,
        firstName: String,
        lastName: String,
        email: String,
        phone: String?,
        role: String,
        companyId: Int
    ) {
        com.prodigit.bodhiattendance.core.monitoring.OfflineWorkSessionStore.clear(preferencesContext)

        // A new interactive login must never inherit another device session.
        SecureAndroidRefreshStore(preferencesContext).clear()
        preferences
            .edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_FIRST_NAME, firstName)
            .putString(KEY_LAST_NAME, lastName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .putString(KEY_ROLE, role)
            .putInt(KEY_COMPANY_ID, companyId)
            .apply()
    }


    // ============================================================
    // GET TOKEN
    // ============================================================

    fun getToken(): String? {

        return preferences.getString(
            KEY_TOKEN,
            null
        )
    }


    fun updateAccessToken(token: String) {
        check(preferences.edit().putString(KEY_TOKEN, token).commit()) {
            "Access token persistence failed"
        }
    }

    // ============================================================
    // CHECK SESSION
    // ============================================================

    fun hasSession(): Boolean {

        return !getToken().isNullOrBlank()
    }


    // ============================================================
    // USER DETAILS
    // ============================================================

    fun getUserId(): Int {

        return preferences.getInt(
            KEY_USER_ID,
            -1
        )
    }


    fun getFirstName(): String? {

        return preferences.getString(
            KEY_FIRST_NAME,
            null
        )
    }


    fun getLastName(): String? {

        return preferences.getString(
            KEY_LAST_NAME,
            null
        )
    }


    fun getEmail(): String? {

        return preferences.getString(
            KEY_EMAIL,
            null
        )
    }


    fun getPhone(): String? {

        return preferences.getString(
            KEY_PHONE,
            null
        )
    }


    fun getRole(): String? {

        return preferences.getString(
            KEY_ROLE,
            null
        )
    }


    fun getCompanyId(): Int {

        return preferences.getInt(
            KEY_COMPANY_ID,
            -1
        )
    }

    /** Persist only a positive company ID verified by the authenticated device API. */
    fun updateCompanyId(companyId: Int) {
        require(companyId > 0) { "Invalid company ID" }
        check(preferences.edit().putInt(KEY_COMPANY_ID, companyId).commit()) {
            "Company ID persistence failed"
        }
    }
    //===================================================================
    //                              Session Validation
    //===================================================================
    fun hasValidSession(): Boolean {

        val token = getToken()

        if (token.isNullOrBlank()) {
            return false
        }

        if (JwtUtils.isTokenExpired(token)) {

            // Expiry must not erase the device identity or offline queue.
            return false
        }

        return true
    }

    // ============================================================
    // CLEAR SESSION / LOGOUT
    // ============================================================

    fun clearSession() {
        com.prodigit.bodhiattendance.core.monitoring.OfflineWorkSessionStore.clear(preferencesContext, "Signed out — offline authorization removed")
        preferencesContext.stopService(android.content.Intent(preferencesContext, com.prodigit.bodhiattendance.core.monitoring.AttendancePresenceService::class.java))

        SecureAndroidRefreshStore(preferencesContext).clear()
        preferences
            .edit()
            .clear()
            .apply()
    }


    companion object {

        private const val PREF_NAME =
            "prodigit_attendance_session"

        private const val KEY_TOKEN =
            "auth_token"

        private const val KEY_USER_ID =
            "user_id"

        private const val KEY_FIRST_NAME =
            "first_name"

        private const val KEY_LAST_NAME =
            "last_name"

        private const val KEY_EMAIL =
            "email"

        private const val KEY_PHONE =
            "phone"

        private const val KEY_ROLE =
            "role"

        private const val KEY_COMPANY_ID =
            "company_id"
    }
}