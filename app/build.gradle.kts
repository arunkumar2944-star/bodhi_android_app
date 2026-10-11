
plugins {

    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.compose)

    // Room annotation/code generation
    alias(libs.plugins.ksp)
}


android {

    namespace =
        "com.prodigit.bodhiattendance"


    compileSdk {

        version =
            release(37)
    }


    defaultConfig {

        applicationId =
            "com.prodigit.bodhiattendance"

        minSdk =
            26

        targetSdk =
            37

        versionCode =
            1

        versionName =
            "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {

        debug {

            buildConfigField(
                "String",
                "BASE_URL",
                "\"http://127.0.0.1:5000/\""
            )
        }

        release {

            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://crm-umber-eta.vercel.app/\""
            )

            optimization {
                enable = false
            }
        }
    }


    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }


    buildFeatures {

        compose =
            true

        buildConfig =
            true
    }



}

// ============================================================
// IMPORTANT:
// androidComponents is OUTSIDE android { }
// ============================================================

androidComponents {

    onVariants { variant ->

        variant.outputs.forEach { output ->

            val apkName =
                if (variant.buildType == "debug") {

                    "BodhiAttendance-Prototype.apk"

                } else {

                    "BodhiAttendance-Release.apk"
                }

            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName
                ?.set(apkName)
        }
    }
}


dependencies {


    // ============================================================
    // COMPOSE BOM
    // ============================================================

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )


    // ============================================================
    // COIL
    // ============================================================

    implementation(
        "io.coil-kt.coil3:coil-compose:3.3.0"
    )

    implementation(
        "io.coil-kt.coil3:coil-gif:3.3.0"
    )


    // ============================================================
    // ROOM / SQLITE OFFLINE STORAGE
    // ============================================================

    implementation(
        libs.androidx.room.runtime
    )

    implementation(
        libs.androidx.room.ktx
    )

    ksp(
        libs.androidx.room.compiler
    )


    // ============================================================
    // COMPOSE
    // ============================================================

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )


    // ============================================================
    // ANDROID CORE
    // ============================================================

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )


    // ============================================================
    // SPLASH SCREEN
    // ============================================================

    implementation(
        "androidx.core:core-splashscreen:1.0.1"
    )


    // ============================================================
    // MATERIAL ICONS
    // ============================================================

    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    // ============================================================
    // VIEWMODEL + COMPOSE
    // ============================================================

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4"
    )


    // ============================================================
    // RETROFIT
    // ============================================================

    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )


    // ============================================================
    // NAVIGATION
    // ============================================================

    implementation(
        libs.androidx.navigation.compose
    )


    // ============================================================
    // UNIT TESTING
    // ============================================================

    testImplementation(
        libs.junit
    )


    // ============================================================
    // ANDROID TESTING
    // ============================================================

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )


    // ============================================================
    // DEBUG
    // ============================================================

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}