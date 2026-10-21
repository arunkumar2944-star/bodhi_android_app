package com.prodigit.bodhiattendance.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.prodigit.bodhiattendance.R


@Composable
fun AppBackground(
    content: @Composable BoxScope.() -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (isSystemInDarkTheme() && MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color(0xFF101827)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        } else {
            Image(
                painter = painterResource(id = R.drawable.launch_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        content()
    }
}