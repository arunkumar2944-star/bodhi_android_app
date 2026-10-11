package com.prodigit.bodhiattendance.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

import com.prodigit.bodhiattendance.BuildConfig
import com.prodigit.bodhiattendance.config.AppConfig


@Composable
fun AppVersionText(

    modifier: Modifier = Modifier,

    textColor: Color =
        Color(0xFF5F6F89),

    fontSize: TextUnit =
        11.sp,

    showPoweredBy: Boolean =
        true

) {

    val appName =
        stringResource(
            id = AppConfig.appNameRes
        )

    val poweredBy =
        stringResource(
            id = AppConfig.poweredByRes
        )

    val versionName =
        BuildConfig.VERSION_NAME


    Column(

        modifier =
            modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Text(

            text =
                "$appName  •  v$versionName",

            fontSize =
                fontSize,

            fontWeight =
                FontWeight.Medium,

            color =
                textColor
        )


        if (showPoweredBy) {

            Text(

                text =
                    poweredBy,

                fontSize =
                    10.sp,

                color =
                    textColor.copy(
                        alpha = 0.75f
                    )
            )
        }
    }
}