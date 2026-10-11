package com.prodigit.bodhiattendance.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prodigit.bodhiattendance.config.AppConfig


@Composable
fun AppLogoHeader(

    modifier: Modifier = Modifier,

    size: Dp = 80.dp

) {

    Image(

        painter =
            painterResource(
                id = AppConfig.appLogoRes
            ),

        contentDescription =
            stringResource(
                id = AppConfig.appNameRes
            ),

        modifier =
            modifier.size(
                size
            )
    )
}