package com.prodigit.bodhiattendance.config

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.prodigit.bodhiattendance.R

object AppConfig {

    // ========================================================
    // BRANDING
    // ========================================================

    @StringRes
    val appNameRes: Int =
        R.string.app_name

    @StringRes
    val appSubtitleRes: Int =
        R.string.app_subtitle

    @StringRes
    val poweredByRes: Int =
        R.string.powered_by


    // ========================================================
    // LOGO
    // ========================================================

    @DrawableRes
    val appLogoRes: Int =
        R.drawable.bodhi_multiverse_logo_transparent
}