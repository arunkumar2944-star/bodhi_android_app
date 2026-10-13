package com.prodigit.bodhiattendance.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prodigit.bodhiattendance.BuildConfig
import com.prodigit.bodhiattendance.R
import kotlinx.coroutines.delay


// =============================================================
// BODHI LAUNCH SCREEN
// =============================================================

@Composable
fun BodhiLaunchScreen(
    networkError: Boolean = false
) {

    // =========================================================
    // ENTRANCE ANIMATION VALUES
    // =========================================================

    val titleAlpha =
        remember {
            Animatable(0f)
        }

    val titleOffset =
        remember {
            Animatable(20f)
        }


    val subtitleAlpha =
        remember {
            Animatable(0f)
        }

    val subtitleOffset =
        remember {
            Animatable(20f)
        }


    val logoAlpha =
        remember {
            Animatable(0f)
        }

    val logoOffset =
        remember {
            Animatable(25f)
        }


    val bottomAlpha =
        remember {
            Animatable(0f)
        }

    val bottomOffset =
        remember {
            Animatable(25f)
        }


    // =========================================================
    // START ENTRANCE ANIMATION
    // =========================================================

    LaunchedEffect(Unit) {

        // -----------------------------------------------------
        // 1. TITLE
        // -----------------------------------------------------

        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            )
        )


        titleOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            )
        )


        delay(250)


        // -----------------------------------------------------
        // 2. SUBTITLE + CATEGORIES
        // -----------------------------------------------------

        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            )
        )


        subtitleOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            )
        )


        delay(250)


        // -----------------------------------------------------
        // 3. MAIN LOGO
        // -----------------------------------------------------

        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )


        logoOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )


        delay(300)


        // -----------------------------------------------------
        // 4. BOTTOM LOADING SECTION
        // -----------------------------------------------------

        bottomAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            )
        )


        bottomOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            )
        )
    }


    // =========================================================
    // IMPORTANT
    // =========================================================
    //
    // Navigation is NOT handled here.
    //
    // StartupViewModel handles:
    //
    // 1. 9-second minimum loading period
    // 2. Internet availability
    // 3. Session validation
    //
    // AppNavigation handles:
    //
    // LOGIN
    // ONBOARDING
    // DASHBOARD
    //
    // =========================================================


    // =========================================================
    // SCREEN
    // =========================================================

    Box(
        modifier = Modifier.fillMaxSize()
    ) {


        // =====================================================
        // BACKGROUND
        // =====================================================

        Image(
            painter = painterResource(
                id = R.drawable.launch_background
            ),

            contentDescription = null,

            modifier =
                Modifier.fillMaxSize(),

            contentScale =
                ContentScale.FillBounds
        )


        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 24.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {


            // =================================================
            // TOP TITLE SECTION
            // =================================================

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,

                modifier =
                    Modifier
                        .padding(
                            top = 20.dp,
                            bottom = 18.dp
                        )
                        .offset(
                            y = titleOffset.value.dp
                        )
            ) {


                // -------------------------------------------------
                // BODHI MULTIVERSE
                // -------------------------------------------------

                Text(
                    text =
                        "BODHI MULTIVERSE",

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.ExtraBold,

                    letterSpacing =
                        1.5.sp,

                    color =
                        Color(0xFF145A32)
                            .copy(
                                alpha =
                                    titleAlpha.value
                            ),

                    textAlign =
                        TextAlign.Center,

                    maxLines =
                        1,

                    softWrap =
                        false
                )


                // =================================================
                // SUBTITLE + CATEGORY
                // =================================================

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    modifier =
                        Modifier.offset(
                            y =
                                subtitleOffset.value.dp
                        )
                ) {


                    // -------------------------------------------------
                    // MOTTO
                    // -------------------------------------------------

                    Text(
                        text =
                            "Multiple Universe, One Vision",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            0.3.sp,

                        color =
                            Color(0xFF4A5568)
                                .copy(
                                    alpha =
                                        subtitleAlpha.value
                                ),

                        textAlign =
                            TextAlign.Center,

                        modifier =
                            Modifier.padding(
                                top = 3.dp
                            )
                    )


                    // -------------------------------------------------
                    // CATEGORY
                    // -------------------------------------------------

                    Text(
                        text =
                            "TECHNOLOGY  -  NATURE  -  EDUCATION",

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            1.2.sp,

                        color =
                            Color(0xFF1976D2)
                                .copy(
                                    alpha =
                                        subtitleAlpha.value
                                ),

                        textAlign =
                            TextAlign.Center,

                        modifier =
                            Modifier.padding(
                                top = 8.dp
                            )
                    )
                }
            }


            // =================================================
            // MAIN BODHI MULTIVERSE LOGO
            // =================================================

            Image(
                painter =
                    painterResource(
                        id =
                            R.drawable
                                .bodhi_multiverse_logo_transparent
                    ),

                contentDescription =
                    null,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .offset(
                            y =
                                (-25).dp +
                                        logoOffset.value.dp
                        ),

                contentScale =
                    ContentScale.Fit,

                alpha =
                    logoAlpha.value
            )


            // =================================================
            // BOTTOM LOADING AREA
            // =================================================

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,

                modifier =
                    Modifier
                        .padding(
                            bottom = 45.dp
                        )
                        .offset(
                            y =
                                (-10).dp +
                                        bottomOffset.value.dp
                        )
            ) {


                // =================================================
                // LOADING ORBIT
                // =================================================

                AsyncImage(
                    model =
                        R.drawable.loading_orbit,

                    contentDescription =
                        null,

                    modifier =
                        Modifier
                            .width(380.dp)
                            .height(315.dp)
                            .offset(
                                y = (-25).dp
                            ),

                    contentScale =
                        ContentScale.Fit,

                    alpha =
                        0.90f *
                                bottomAlpha.value
                )


                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )


                // =================================================
                // LOADING / NETWORK STATUS
                // =================================================

                // Keep the exact same space for loading and error messages.
                // Without a stable container the taller network-error text remeasures
                // the Column and makes the animated orbit appear to jump.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (networkError) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Network Error",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFFD32F2F).copy(alpha = bottomAlpha.value),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Please check your internet connection",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp,
                                color = Color(0xFFD32F2F).copy(alpha = bottomAlpha.value),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Waiting for network...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp,
                                color = Color(0xFF455A64).copy(alpha = bottomAlpha.value * 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LoadingTextAnimation(alphaMultiplier = bottomAlpha.value)
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                // =================================================
                // SYSTEM NAME + VERSION
                // =================================================

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.Center,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {


                    // -------------------------------------------------
                    // APP NAME
                    // -------------------------------------------------

                    Text(
                        text =
                            stringResource(
                                R.string.app_name
                            ),

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.ExtraBold,

                        letterSpacing =
                            0.8.sp,

                        color =
                            Color.Black.copy(
                                alpha =
                                    bottomAlpha.value
                            ),

                        textAlign =
                            TextAlign.Center
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    // -------------------------------------------------
                    // SEPARATOR
                    // -------------------------------------------------

                    Text(
                        text = "-",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.ExtraBold,

                        color =
                            Color.Black.copy(
                                alpha =
                                    bottomAlpha.value *
                                            0.45f
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    // -------------------------------------------------
                    // APP VERSION
                    // -------------------------------------------------

                    Text(
                        text =
                            "v${BuildConfig.VERSION_NAME}",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.ExtraBold,

                        color =
                            Color.Black.copy(
                                alpha =
                                    bottomAlpha.value *
                                            0.55f
                            )
                    )
                }
            }
        }
    }
}


// =============================================================
// LOADING TEXT ANIMATION
// =============================================================

@Composable
fun LoadingTextAnimation(
    alphaMultiplier: Float = 1f
) {


    // =========================================================
    // LOADING TEXT PULSE
    // =========================================================

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "loading_text"
        )


    val alpha by
    infiniteTransition.animateFloat(

        initialValue =
            0.55f,

        targetValue =
            1f,

        animationSpec =
            infiniteRepeatable(

                animation =
                    tween(
                        durationMillis =
                            900
                    ),

                repeatMode =
                    RepeatMode.Reverse
            ),

        label =
            "loading_alpha"
    )


    // =========================================================
    // DOT ANIMATION
    // =========================================================

    var dots by
    remember {
        mutableStateOf("")
    }


    LaunchedEffect(Unit) {

        while (true) {

            dots =
                when (dots) {

                    "" ->
                        "."

                    "." ->
                        ".."

                    ".." ->
                        "..."

                    "..." ->
                        ""

                    else ->
                        ""
                }


            delay(500)
        }
    }


    // =========================================================
    // LOADING + DOTS
    // =========================================================

    Row(
        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.Center
    ) {


        Text(
            text =
                "Loading",

            fontSize =
                18.sp,

            fontWeight =
                FontWeight.ExtraBold,

            letterSpacing =
                1.1.sp,

            color =
                Color(0xFF0D47A1)
                    .copy(
                        alpha =
                            alpha *
                                    alphaMultiplier
                    )
        )


        // Fixed width prevents Loading text
        // from moving when dots change.

        Box(
            modifier =
                Modifier.width(30.dp)
        ) {

            Text(
                text =
                    dots,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.ExtraBold,

                letterSpacing =
                    1.1.sp,

                color =
                    Color(0xFFFF9800)
                        .copy(
                            alpha =
                                alphaMultiplier
                        )
            )
        }
    }
}