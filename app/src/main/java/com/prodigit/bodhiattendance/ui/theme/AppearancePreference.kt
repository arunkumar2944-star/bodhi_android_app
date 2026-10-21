package com.prodigit.bodhiattendance.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI-only preference. Never stores attendance evidence or alters background monitoring.
 * SYSTEM is the default; previous installs remain on the device theme until changed.
 */
object AppearancePreference {
    private const val FILE = "bodhi_appearance"
    private const val KEY = "theme_mode"
    private val mode = MutableStateFlow("SYSTEM")
    private var loaded = false

    @Synchronized
    fun observe(context: Context): StateFlow<String> {
        if (!loaded) {
            val saved = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getString(KEY, "SYSTEM")
            mode.value = if (saved in listOf("LIGHT", "DARK", "SYSTEM")) saved!! else "SYSTEM"
            loaded = true
        }
        return mode
    }

    fun set(context: Context, value: String) {
        require(value in listOf("LIGHT", "DARK", "SYSTEM"))
        observe(context)
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()
        mode.value = value
    }
}
