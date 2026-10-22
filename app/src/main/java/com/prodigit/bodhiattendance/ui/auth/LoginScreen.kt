package com.prodigit.bodhiattendance.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prodigit.bodhiattendance.BuildConfig
import com.prodigit.bodhiattendance.R


// ============================================================
// BODHI THEME COLORS
// ============================================================

private val BodhiNavy: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val BodhiBlue: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val BodhiBlueLight: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val InputBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val SuccessGreen = Color(0xFF64846E)


// ============================================================
// LOGIN SCREEN
// ============================================================

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {}
) {

    // ========================================================
    // VIEWMODEL STATE
    // ========================================================

    val context = androidx.compose.ui.platform.LocalContext.current
    val state = loginViewModel.uiState


    // ========================================================
    // HANDLE SUCCESSFUL LOGIN
    // ========================================================

    LaunchedEffect(state.isLoginSuccessful) {

        if (state.isLoginSuccessful) {

            onLoginSuccess()

            loginViewModel.onLoginNavigationHandled()
        }
    }


    // ========================================================
    // RESPONSIVE PHONE HEIGHT
    // ========================================================

    val configuration = LocalConfiguration.current

    val screenHeightDp =
        configuration.screenHeightDp

    val isVerySmallScreen =
        screenHeightDp < 620

    val isSmallScreen =
        screenHeightDp < 700


    // ========================================================
    // RESPONSIVE SPACING
    // ========================================================

    val topSpace =
        when {
            isVerySmallScreen -> 16.dp
            isSmallScreen -> 22.dp
            else -> 28.dp
        }

    val logoSize =
        when {
            isVerySmallScreen -> 72.dp
            isSmallScreen -> 80.dp
            else -> 88.dp
        }

    val welcomeSpacing =
        when {
            isVerySmallScreen -> 16.dp
            isSmallScreen -> 20.dp
            else -> 24.dp
        }

    val cardTopSpacing =
        when {
            isVerySmallScreen -> 10.dp
            isSmallScreen -> 12.dp
            else -> 15.dp
        }


    // ========================================================
    // LOGIN CARD RESPONSIVE VALUES
    // ========================================================

    val cardVerticalPadding =
        when {
            isVerySmallScreen -> 15.dp
            isSmallScreen -> 18.dp
            else -> 21.dp
        }

    val fieldSpacing =
        when {
            isVerySmallScreen -> 9.dp
            isSmallScreen -> 10.dp
            else -> 12.dp
        }

    val buttonTopSpacing =
        when {
            isVerySmallScreen -> 12.dp
            isSmallScreen -> 14.dp
            else -> 17.dp
        }

    val buttonHeight =
        when {
            isVerySmallScreen -> 44.dp
            isSmallScreen -> 47.dp
            else -> 50.dp
        }

    val footerSpacing =
        when {
            isVerySmallScreen -> 9.dp
            isSmallScreen -> 11.dp
            else -> 13.dp
        }

    val versionSpacing =
        when {
            isVerySmallScreen -> 5.dp
            isSmallScreen -> 7.dp
            else -> 9.dp
        }


    // ========================================================
    // APP NAME + VERSION
    // ========================================================

    val appName =
        stringResource(R.string.app_name)

    val versionName =
        BuildConfig.VERSION_NAME


    // ========================================================
    // MAIN SCREEN
    // ========================================================

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // ====================================================
        // BODHI BACKGROUND
        // ====================================================

        Image(
            painter = painterResource(
                id = R.drawable.launch_background
            ),

            contentDescription = null,

            modifier = Modifier.fillMaxSize(),

            contentScale = ContentScale.Crop
        )


        // ====================================================
        // TOP BLUE ATMOSPHERE
        // ====================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    when {
                        isVerySmallScreen -> 190.dp
                        isSmallScreen -> 210.dp
                        else -> 230.dp
                    }
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BodhiBlue.copy(
                                alpha = 0.10f
                            ),

                            BodhiBlueLight.copy(
                                alpha = 0.045f
                            ),

                            Color.Transparent
                        )
                    )
                )
        )


        // ====================================================
        // MAIN CONTENT
        // ====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 18.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            // =================================================
            // TOP SPACE
            // =================================================

            Spacer(
                modifier =
                    Modifier.height(topSpace)
            )


            // =================================================
            // BODHI LOGO
            // =================================================

            Image(
                painter = painterResource(
                    id =
                        R.drawable
                            .bodhi_multiverse_logo_transparent
                ),

                contentDescription =
                    "BODHI Multiverse",

                modifier =
                    Modifier.size(logoSize)
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // =================================================
            // APP NAME
            // =================================================

            Text(
                text =
                    appName.uppercase(),

                fontSize =
                    if (isVerySmallScreen) {
                        18.sp
                    } else {
                        20.sp
                    },

                fontWeight =
                    FontWeight.Bold,

                letterSpacing =
                    1.1.sp,

                color =
                    BodhiNavy,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )


            // =================================================
            // TAGLINE
            // =================================================

            Text(
                text =
                    "Multiple Universe, One Vision",

                fontSize =
                    10.sp,

                letterSpacing =
                    0.35.sp,

                color =
                    BodhiNavy.copy(
                        alpha = 0.72f
                    ),

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(welcomeSpacing)
            )


            // =================================================
            // WELCOME SECTION
            // =================================================

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text =
                        "Welcome back",

                    fontSize =
                        if (isVerySmallScreen) {
                            24.sp
                        } else {
                            27.sp
                        },

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        BodhiNavy
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Sign in to continue to your workspace.",

                    fontSize =
                        12.sp,

                    color =
                        BodhiNavy.copy(
                            alpha = 0.70f
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.height(cardTopSpacing)
            )


            // =================================================
            // LOGIN CARD
            // =================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(24.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White.copy(
                                alpha = 0.97f
                            )
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 7.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical =
                                    cardVerticalPadding
                            )
                ) {


                    // =========================================
                    // SIGN IN TITLE
                    // =========================================

                    Text(
                        text =
                            "Sign in",

                        fontSize =
                            23.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            BodhiNavy
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            "Enter your credentials to continue",

                        fontSize =
                            11.sp,

                        color =
                            Color.Gray
                    )


                    // =========================================
                    // LOGIN ERROR
                    // =========================================

                    state.loginError?.let { message ->

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                message,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(19.dp)
                    )


                    // =========================================
                    // EMAIL / PHONE LABEL
                    // =========================================

                    Text(
                        text =
                            "Email or Phone Number",

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            BodhiNavy
                    )


                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )


                    // =========================================
                    // EMAIL / PHONE FIELD
                    // =========================================

                    OutlinedTextField(
                        value =
                            state.emailOrPhone,

                        onValueChange =
                            loginViewModel
                            ::onEmailOrPhoneChange,

                        placeholder = {

                            Text(
                                text =
                                    "Enter Email or Phone Number",

                                fontSize =
                                    12.sp
                            )
                        },

                        leadingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Person,

                                contentDescription =
                                    null,

                                tint =
                                    BodhiNavy.copy(
                                        alpha = 0.65f
                                    )
                            )
                        },

                        isError =
                            state.emailOrPhoneError != null,

                        supportingText = {

                            state.emailOrPhoneError?.let {
                                    error ->

                                Text(
                                    text =
                                        error,

                                    fontSize =
                                        11.sp,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                )
                            }
                        },

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(11.dp),

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Text
                            ),

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(

                                    focusedBorderColor =
                                        BodhiBlue,

                                    unfocusedBorderColor =
                                        InputBorder,

                                    focusedContainerColor =
                                        Color.White,

                                    unfocusedContainerColor =
                                        Color.White
                                ),

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    Spacer(
                        modifier =
                            Modifier.height(fieldSpacing)
                    )


                    // =========================================
                    // PASSWORD LABEL
                    // =========================================

                    Text(
                        text =
                            "Password",

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            BodhiNavy
                    )


                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )


                    // =========================================
                    // PASSWORD FIELD
                    // =========================================

                    OutlinedTextField(
                        value =
                            state.password,

                        onValueChange =
                            loginViewModel
                            ::onPasswordChange,

                        placeholder = {

                            Text(
                                text =
                                    "Enter your password",

                                fontSize =
                                    12.sp
                            )
                        },

                        leadingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Lock,

                                contentDescription =
                                    null,

                                tint =
                                    BodhiNavy.copy(
                                        alpha = 0.65f
                                    )
                            )
                        },

                        trailingIcon = {

                            IconButton(
                                onClick =
                                    loginViewModel
                                    ::togglePasswordVisibility
                            ) {

                                Icon(
                                    imageVector =
                                        if (
                                            state.passwordVisible
                                        ) {
                                            Icons.Default
                                                .VisibilityOff
                                        } else {
                                            Icons.Default
                                                .Visibility
                                        },

                                    contentDescription =
                                        if (
                                            state.passwordVisible
                                        ) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        },

                                    tint =
                                        BodhiNavy.copy(
                                            alpha = 0.65f
                                        )
                                )
                            }
                        },

                        visualTransformation =
                            if (
                                state.passwordVisible
                            ) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Password
                            ),

                        isError =
                            state.passwordError != null,

                        supportingText = {

                            state.passwordError?.let {
                                    error ->

                                Text(
                                    text =
                                        error,

                                    fontSize =
                                        11.sp,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                )
                            }
                        },

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(11.dp),

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(

                                    focusedBorderColor =
                                        BodhiBlue,

                                    unfocusedBorderColor =
                                        InputBorder,

                                    focusedContainerColor =
                                        Color.White,

                                    unfocusedContainerColor =
                                        Color.White
                                ),

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    // =========================================
                    // FORGOT PASSWORD
                    // =========================================

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )


                    Text(
                        text =
                            "Forgot password?",

                        color =
                            BodhiBlue,

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.End
                                )
                                .clickable {
                                    android.widget.Toast.makeText(context, "Reset your password in the CRM login page or contact your administrator", android.widget.Toast.LENGTH_LONG).show()
                                }
                    )


                    Spacer(
                        modifier =
                            Modifier.height(buttonTopSpacing)
                    )


                    // =========================================
                    // SIGN IN BUTTON
                    // =========================================

                    Button(
                        onClick = {
                            loginViewModel
                                .onLoginClick()
                        },

                        enabled =
                            !state.isLoading,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(buttonHeight),

                        shape =
                            RoundedCornerShape(11.dp),

                        colors =
                            ButtonDefaults
                                .buttonColors(

                                    containerColor =
                                        BodhiBlue,

                                    disabledContainerColor =
                                        BodhiBlue.copy(
                                            alpha = 0.55f
                                        )
                                )
                    ) {

                        if (state.isLoading) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(20.dp),

                                color =
                                    Color.White,

                                strokeWidth =
                                    2.dp
                            )

                        } else {

                            Text(
                                text =
                                    "Sign In",

                                fontSize =
                                    14.sp,

                                fontWeight =
                                    FontWeight.SemiBold
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )


                            Icon(
                                imageVector =
                                    Icons.Default
                                        .ArrowForward,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(17.dp)
                            )
                        }
                    }


                    // =========================================
                    // FOOTER
                    // =========================================

                    Spacer(
                        modifier =
                            Modifier.height(footerSpacing)
                    )


                    HorizontalDivider(
                        color =
                            MaterialTheme.colorScheme.outlineVariant,

                        thickness =
                            1.dp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(footerSpacing)
                    )


                    // =========================================
                    // SECURE ACCESS
                    // =========================================

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.Center,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default
                                    .VerifiedUser,

                            contentDescription =
                                null,

                            tint =
                                SuccessGreen,

                            modifier =
                                Modifier.size(14.dp)
                        )


                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )


                        Text(
                            text =
                                "Secure employee access",

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.Medium,

                            letterSpacing =
                                0.15.sp,

                            color =
                                BodhiNavy.copy(
                                    alpha = 0.62f
                                )
                        )
                    }


                    // =========================================
                    // APP VERSION
                    // =========================================

                    Spacer(
                        modifier =
                            Modifier.height(versionSpacing)
                    )


                    Text(
                        text =
                            "$appName  -  v$versionName",

                        fontSize =
                            when {
                                isVerySmallScreen -> 11.sp
                                isSmallScreen -> 12.sp
                                else -> 13.sp
                            },

                        fontWeight =
                            FontWeight.ExtraBold,

                        letterSpacing =
                            0.5.sp,

                        color =
                            BodhiNavy.copy(
                                alpha = 0.82f
                            ),

                        textAlign =
                            TextAlign.Center,

                        maxLines =
                            1,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            }


            // =================================================
            // BOTTOM SPACE
            // =================================================

            Spacer(
                modifier =
                    Modifier.height(
                        if (isVerySmallScreen) {
                            12.dp
                        } else {
                            18.dp
                        }
                    )
            )
        }
    }
}