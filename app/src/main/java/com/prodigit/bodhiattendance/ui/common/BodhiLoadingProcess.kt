package com.prodigit.bodhiattendance.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.prodigit.bodhiattendance.R
import com.prodigit.bodhiattendance.ui.launch.LoadingTextAnimation

/**
 * Standard branded loading process matching the launch screen orbit loading experience.
 */
@Composable
fun BodhiOrbitLoadingComponent(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = R.drawable.loading_orbit,
            contentDescription = null,
            modifier = Modifier
                .width(130.dp)
                .height(90.dp),
            contentScale = ContentScale.Fit,
            alpha = 0.92f
        )
        Spacer(modifier = Modifier.height(4.dp))
        LoadingTextAnimation(alphaMultiplier = 1f)
        if (message != "Loading...") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Modal branded loading overlay dialog using launch screen orbit process.
 */
@Composable
fun BodhiOrbitLoadingDialog(
    message: String = "Processing..."
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            BodhiOrbitLoadingComponent(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 200.dp),
                message = message
            )
        }
    }
}
